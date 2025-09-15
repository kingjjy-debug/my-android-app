import fs from "fs";
import path from "path";
import { GoogleGenerativeAI } from "@google/generative-ai";
const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY);

function safeParseJson(raw){
  let s=String(raw??"").trim();
  s=s.replace(/^```json\s*/i,"").replace(/^```\s*/i,"").replace(/```$/i,"").trim();
  try{return JSON.parse(s);}catch{}
  const a=s.indexOf("{"), b=s.lastIndexOf("}");
  if(a!==-1 && b!==-1 && b>a){ try{return JSON.parse(s.slice(a,b+1));}catch{} }
  try{return JSON.parse(s.replace(/,\s*([}\]])/g,"$1"));}catch{}
  throw new Error("Model output is not valid JSON");
}
function norm(p){
  const posix=path.posix;
  if(p.startsWith("app/")) return p;
  if(p.startsWith("src/main/")) return posix.join("app",p);
  if(p==="AndroidManifest.xml") return "app/src/main/AndroidManifest.xml";
  return posix.join("app/src/main",p);
}
function ensure(fp){ fs.mkdirSync(path.dirname(fp),{recursive:true}); }
function pkgToPath(pkg){ return pkg.replace(/\./g,"/"); }
function extractArray(desc){
  const a=desc.indexOf("["), b=desc.lastIndexOf("]");
  if(a===-1||b===-1||b<=a) return null;
  try{ const arr=JSON.parse(desc.slice(a,b+1)); return Array.isArray(arr)?arr:null; }catch{ return null; }
}

/** 간단 검증기: 요구 위반 사항을 배열로 반환 */
function validate(root, contract, dataArr){
  const errs=[];
  const pkg=contract.packageName;
  const PKG_PATH=pkgToPath(pkg);

  // 파일 존재
  for(const rf of contract.requiredFiles||[]){
    const p=rf.replace("<PKG_PATH>",`app/src/main/java/${PKG_PATH}`);
    if(!fs.existsSync(path.join(root,p))) errs.push(`필수 파일 누락: ${p}`);
  }

  // View ID 존재
  for(const [fp,ids] of Object.entries(contract.requiredViewIds||{})){
    const full=path.join(root,fp);
    if(!fs.existsSync(full)){ errs.push(`레이아웃 누락: ${fp}`); continue; }
    const xml=fs.readFileSync(full,"utf8");
    for(const id of ids){
      if(!xml.includes(`@+id/${id}`) && !xml.includes(`@id/${id}`)){
        errs.push(`ID 누락(${fp}): ${id}`);
      }
    }
  }

  // 패키지명
  const mn=path.join(root,`app/src/main/java/${PKG_PATH}/MainActivity.kt`);
  if(!fs.existsSync(mn)) errs.push("MainActivity.kt 누락/경로오류");

  // 버튼 한국어 라벨(대략 검사)
  const item = path.join(root,"app/src/main/res/layout/item_card.xml");
  if(fs.existsSync(item)){
    const x=fs.readFileSync(item,"utf8");
    if(!/포인트 적립하기|마일리지 전환하기/.test(x)) errs.push("버튼 한국어 라벨 누락(아이템 카드)");
  }

  // 상태 저장/리셋 논리 존재(단순 정적 검사)
  if(fs.existsSync(mn)){
    const k=fs.readFileSync(mn,"utf8");
    if(!/SharedPreferences/.test(k)) errs.push("SharedPreferences 미사용");
    if(!/yyyy-MM-dd/.test(k) && !/LocalDate/.test(k)) errs.push("날짜 기반 리셋 로직 흔적 없음");
  }

  // 설명서 데이터 개수 반영 (Data.kt 안 키 개수와 비교)
  if(dataArr?.length){
    const dk=path.join(root,`app/src/main/java/${PKG_PATH}/Data.kt`);
    if(fs.existsSync(dk)){
      const s=fs.readFileSync(dk,"utf8");
      const keysInData = (s.match(/Item\(/g)||[]).length;
      if(keysInData < dataArr.length) errs.push(`데이터 항목 수 부족: 설명서 ${dataArr.length}개, 코드 ${keysInData}개`);
    }
  }

  // "팁 목록" 류 화면 억제: MainActivity에 RecyclerView/Adapter 사용 여부
  if(fs.existsSync(mn)){
    const s=fs.readFileSync(mn,"utf8");
    if(!/RecyclerView|ItemAdapter/.test(s)) errs.push("RecyclerView/Adapter 미사용(팁 리스트 화면 의심)");
  }

  return errs;
}

async function callModel(model, prompt){
  const resp = await model.generateContent(prompt);
  return resp.response.text();
}

async function main(){
  const ROOT=process.cwd();
  const descPath=process.argv[2]||"app_description.txt";
  const description=fs.readFileSync(descPath,"utf8");
  const contract=JSON.parse(fs.readFileSync(path.join(ROOT,"tools","contract.json"),"utf8"));
  const model=genAI.getGenerativeModel({model:contract.model||"gemini-2.5-pro"});
  const pkg=contract.packageName, PKG_PATH=pkgToPath(pkg);
  const dataArr=extractArray(description);
  const dataJSON=dataArr?JSON.stringify(dataArr,null,2):"[]";

  const reqFiles=(contract.requiredFiles||[]).map(f=>f.replace("<PKG_PATH>",`app/src/main/java/${PKG_PATH}`));
  const schema=`
아래 요구사항을 만족하는 안드로이드 앱 코드를 JSON 하나로만 출력하세요.

[고정 규칙]
- 패키지명: ${pkg}
- 모든 경로는 app/src/main/ 하위.
- 필수 파일:
${reqFiles.map(f=>`  - ${f}`).join("\n")}
- 레이아웃 ID 요건:
${Object.entries(contract.requiredViewIds||{}).map(([fp,ids])=>`  - ${fp}: ${ids.join(", ")}`).join("\n")}
- strings.xml 은 앱 이름 포함.
- styles.xml 은 @style/Theme.MyApp 정의, 매니페스트에 적용.
- MainActivity.kt 에 RecyclerView(id=list) 사용 + 어댑터 분리.
- SharedPreferences 로 항목별 완료 상태 저장, 날짜 바뀌면 초기화.

[데이터(설명서에서 추출된 배열)]
${dataJSON}

[설명서 원문]
${description}

출력 포맷(반드시 그대로):
{"files":[{"path":"...","content":"..."}],"notes":"선택"}  // JSON만, 코드펜스/주석 금지
`;

  let lastRaw="";
  for(let attempt=1; attempt<=4; attempt++){
    if(attempt>1) console.log(`== Codegen attempt ${attempt}/4 ==`);
    const raw = await callModel(model, schema);
    lastRaw = raw;
    let obj;
    try{ obj=safeParseJson(raw); }
    catch(e){
      if(attempt===4){ console.error("Code JSON parse fail. Raw:\n", lastRaw); throw e; }
      continue;
    }
    // 파일 쓰기
    for(const f of (obj.files||[])){
      const out=norm(String(f.path||"").trim());
      ensure(out);
      fs.writeFileSync(out,String(f.content??""),"utf8");
      console.log(`Wrote: ${out}`);
    }
    if(obj.notes){
      fs.writeFileSync("codegen_notes.txt",String(obj.notes),"utf8");
      console.log("Wrote: codegen_notes.txt");
    }

    // 검증
    const errs=validate(ROOT,contract,dataArr);
    if(errs.length===0){
      console.log("\n✅ Code generation done. (validated)");
      return;
    }

    // 수정 요청 프롬프트(비판적 피드백)
    const critic = `
다음 위반 사항을 모두 고치고, 전체 코드를 다시 JSON 하나로 출력하세요.
위반 목록:
- ${errs.join("\n- ")}

반드시 이전 규칙과 동일하게 경로/ID/패키지/상태저장/날짜초기화를 지키세요.
JSON만 출력.`;
    const fixRaw = await callModel(model, critic);
    lastRaw = fixRaw;
    try{
      const fix = safeParseJson(fixRaw);
      for(const f of (fix.files||[])){
        const out=norm(String(f.path||"").trim());
        ensure(out);
        fs.writeFileSync(out,String(f.content??""),"utf8");
        console.log(`Wrote: ${out}`);
      }
      if(fix.notes){
        fs.writeFileSync("codegen_notes.txt",String(fix.notes),"utf8");
        console.log("Wrote: codegen_notes.txt");
      }
      const reErr=validate(ROOT,contract,dataArr);
      if(reErr.length===0){ console.log("\n✅ Code generation done. (validated)"); return; }
      // 다음 루프로
    }catch(e){
      if(attempt===4){ console.error("Fix JSON parse fail. Raw:\n", lastRaw); throw e; }
    }
  }
  console.error("❌ 생성/수정 루프를 마쳤지만 여전히 기준 미충족.");
  process.exit(1);
}

process.on("unhandledRejection",e=>{console.error("UnhandledRejection:",e);process.exit(1);});
process.on("uncaughtException",e=>{console.error("UncaughtException:",e);process.exit(1);});
await main();
