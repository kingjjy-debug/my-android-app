import fs from "fs";
import path from "path";
import { GoogleGenerativeAI } from "@google/generative-ai";

const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY);

/** 모델 출력에서 JSON만 안전하게 뽑아내기 */
function safeParseJson(raw) {
  let s = String(raw ?? "").trim();

  // 코드펜스 제거
  s = s.replace(/^```json\s*/i, "")
       .replace(/^```\s*/i, "")
       .replace(/```$/i, "").trim();

  // 1) 바로 파싱
  try { return JSON.parse(s); } catch {}

  // 2) 첫 '{' ~ 마지막 '}' 범위
  const first = s.indexOf("{");
  const last  = s.lastIndexOf("}");
  if (first !== -1 && last !== -1 && last > first) {
    const mid = s.slice(first, last + 1);
    try { return JSON.parse(mid); } catch {}
  }

  // 3) 흔한 꼬리쉼표 제거
  const noTrailing = s.replace(/,\s*([}\]])/g, "$1");
  try { return JSON.parse(noTrailing); } catch {}

  throw new Error("Model output is not valid JSON");
}

/** 경로를 반드시 app/src/main 하위로 강제 */
function normalizeOutPath(p) {
  const posix = path.posix;
  if (p.startsWith("app/")) return p;
  if (p.startsWith("src/main/")) return posix.join("app", p);
  if (p === "AndroidManifest.xml") return "app/src/main/AndroidManifest.xml";
  return posix.join("app/src/main", p);
}

function ensureDirs(fp) {
  fs.mkdirSync(path.dirname(fp), { recursive: true });
}

async function main() {
  const descPath = process.argv[2] || "app_description.txt";
  const prompt = fs.readFileSync(descPath, "utf8");

  const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" });

  const schema = `
당신은 안드로이드 앱 코드 생성기입니다.
반드시 **아래 JSON 하나만** 출력하세요. (설명/마크다운/코드펜스 금지)

{
  "files": [
    {"path": "app/src/main/AndroidManifest.xml", "content": "<xml...>"},
    {"path": "app/src/main/java/com/example/myapplication/MainActivity.kt", "content": "..."},
    {"path": "app/src/main/res/layout/activity_main.xml", "content": "..."},
    {"path": "app/src/main/res/values/styles.xml", "content": "..."}
  ],
  "notes": "선택사항"
}

규칙:
- 모든 파일 경로는 반드시 app/src/main/ 하위(혹은 정확히 위 표기)로만.
- JSON 외 텍스트 절대 금지.
`;

  const input = `${schema}\n\n요구사항:\n${prompt}\n\n주의:\n- 경로 강제(app/src/main).\n- JSON 이외 출력 금지.`;

  const resp = await model.generateContent(input);
  const text = resp.response.text();

  let obj;
  try {
    obj = safeParseJson(text);
  } catch (e) {
    console.error("Model did not return valid JSON. Raw output:\n", text);
    throw e;
  }

  const files = Array.isArray(obj.files) ? obj.files : [];
  for (const f of files) {
    const outPath = normalizeOutPath(String(f.path || "").trim());
    ensureDirs(outPath);
    fs.writeFileSync(outPath, String(f.content ?? ""), "utf8");
    console.log(`Wrote: ${outPath}`);
  }

  if (obj.notes) {
    fs.writeFileSync("codegen_notes.txt", String(obj.notes), "utf8");
    console.log("Wrote: codegen_notes.txt");
  }

  console.log("\n✅ Code generation done. Review changes, then build via CI.");
}

// 안전장치: 핸들되지 않은 예외 표시
process.on("unhandledRejection", (e) => {
  console.error("UnhandledRejection:", e);
  process.exit(1);
});
process.on("uncaughtException", (e) => {
  console.error("UncaughtException:", e);
  process.exit(1);
});

await main();
