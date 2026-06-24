import { describe, expect, it } from "vitest";
import { formatDate, formatMinutes, formatTime } from "./format";

describe("formatMinutes", () => {
  it("0分は0mを返す", () => {
    expect(formatMinutes(0)).toBe("0m");
  });

  it("60分未満は分のみ表示する", () => {
    expect(formatMinutes(30)).toBe("30m");
  });

  it("ちょうど60分は1hを返す", () => {
    expect(formatMinutes(60)).toBe("1h");
  });

  it("時間と分の両方がある場合は両方表示する", () => {
    expect(formatMinutes(90)).toBe("1h 30m");
  });

  it("ちょうど120分は2hを返す", () => {
    expect(formatMinutes(120)).toBe("2h");
  });

  it("大きな値も正しく変換する", () => {
    expect(formatMinutes(510)).toBe("8h 30m");
  });
});

describe("formatTime", () => {
  it("ISO文字列からHH:MM形式に変換する", () => {
    const result = formatTime("2024-01-15T09:00:00+09:00");
    expect(result).toBe("09:00");
  });

  it("午後の時刻も24時間表記で表示する", () => {
    const result = formatTime("2024-01-15T18:30:00+09:00");
    expect(result).toBe("18:30");
  });
});

describe("formatDate", () => {
  it("日付文字列をM/D (曜日)形式に変換する", () => {
    const result = formatDate("2024-01-15");
    expect(result).toBe("1/15 (月)");
  });

  it("日曜日が正しく表示される", () => {
    const result = formatDate("2024-01-14");
    expect(result).toBe("1/14 (日)");
  });

  it("土曜日が正しく表示される", () => {
    const result = formatDate("2024-01-13");
    expect(result).toBe("1/13 (土)");
  });
});
