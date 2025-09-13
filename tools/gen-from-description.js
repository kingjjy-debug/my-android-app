// Simple Android code generator using Gemini (no gemini-CLI needed)
// Usage: GEMINI_API_KEY=... node tools/gen-from-description.js app_description.txt

import fs from "fs";
import path from "path";
import process from "process";
import { GoogleGenerativeAI } from "@google/generative-ai";

const apiKey = process.env.GEMINI_API_KEY;
if (!apiKey) {
  console.error("Error: set GEMINI_API_KEY env var first.");
  process.exit(1);
}
const inputPath = process.argv[2] || "app_description.txt";
if (!fs.existsSync(inputPath)) {
  console.error(`Error: ${inputPath} not found.`);
  process.exit(1);
}

const genAI = new GoogleGenerativeAI(apiKey);
// 빠르고 저렴: 1.5-flash / 더 정밀: 1.5-pro
const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" });

const description = fs.readFileSync(inputPath, "utf8");

// 모델에게 “파일 목록 JSON”으로 달라고 요청 (Markdown 금지)
const systemPrompt = `
당신은 안드로이드 앱 코드를 생성하는 도우미입니다.
출력은 반드시 "JSON 하나"로만, 마크다운/설명/코드펜스 금지.

JSON 스키마:
{
  "files": [
    { "path": "<상대경로>", "content": "<파일내용>" },
    ...
  ],
  "notes": "<선택: 생성 시 주의사항 요약>"
}

요구사항:
- Android/Kotlin + AppCompat 사용
- 최소 구성: AndroidManifest.xml, MainActivity.kt, activity_main.xml
- 패키지명: com.example.myapplication
- MainActivity는 버튼(@+id/button)과 텍스트(@+id/textView)를 사용
- 버튼 클릭마다 "안녕하세요! (N)"로 textView 갱신
- Manifest는 package, exported=true, MAIN/LAUNCHER, application theme 포함
- values/styles.xml 에 Theme.MyApp (Theme.AppCompat.Light.NoActionBar 상속)
- Gradle 스크립트는 이미 있으므로 필요 시에만 files에 추가

반드시 위 JSON 형식만 출력.
`;

async function main() {
  const prompt = [
    systemPrompt,
    "",
    "=== 사용자 요구사항(app_description.txt) ===",
    description,
  ].join("\n");

  const result = await model.generateContent(prompt);
  let text = result.response.text().trim();

  // JSON만 남도록 코드펜스/앞뒤 여분 제거
  if (text.startsWith("```")) {
    text = text.replace(/^```(?:json)?\s*/i, "")
               .replace(/```$/, "")
               .trim();
  }

  let parsed;
  try {
    parsed = JSON.parse(text);
  } catch (e) {
    console.error("Model did not return valid JSON. Raw output:\n", text);
    process.exit(1);
  }

  if (!parsed.files || !Array.isArray(parsed.files)) {
    console.error("JSON missing 'files' array.");
    process.exit(1);
  }

  // 파일 쓰기
  for (const f of parsed.files) {
    const outPath = path.join(process.cwd(), f.path);
    fs.mkdirSync(path.dirname(outPath), { recursive: true });
    fs.writeFileSync(outPath, f.content, "utf8");
    console.log("Wrote:", f.path);
  }

  // 요약 노트 저장(선택)
  if (parsed.notes) {
    fs.writeFileSync("codegen_notes.txt", parsed.notes, "utf8");
    console.log("Wrote: codegen_notes.txt");
  }

  console.log("\n✅ Code generation done. Review changes, then build via CI.");
}

main().catch(err => {
  console.error(err);
  process.exit(1);
});
