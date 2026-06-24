import { describe, expect, it } from "vitest";
import { isNonEmpty, isValidEmail, isWithinLength } from "./validators";

describe("isNonEmpty", () => {
  it("空文字列はfalseを返す", () => {
    expect(isNonEmpty("")).toBe(false);
  });

  it("空白のみの文字列はfalseを返す", () => {
    expect(isNonEmpty("   ")).toBe(false);
  });

  it("nullはfalseを返す", () => {
    expect(isNonEmpty(null)).toBe(false);
  });

  it("undefinedはfalseを返す", () => {
    expect(isNonEmpty(undefined)).toBe(false);
  });

  it("数値はfalseを返す", () => {
    expect(isNonEmpty(123)).toBe(false);
  });

  it("文字が含まれる文字列はtrueを返す", () => {
    expect(isNonEmpty("hello")).toBe(true);
  });

  it("前後に空白がある文字列でもtrueを返す", () => {
    expect(isNonEmpty(" hello ")).toBe(true);
  });
});

describe("isValidEmail", () => {
  it("正しい形式のメールアドレスはtrueを返す", () => {
    expect(isValidEmail("user@example.com")).toBe(true);
  });

  it("サブドメイン付きのメールはtrueを返す", () => {
    expect(isValidEmail("user@sub.example.com")).toBe(true);
  });

  it("@がないとfalseを返す", () => {
    expect(isValidEmail("userexample.com")).toBe(false);
  });

  it("ドメインがないとfalseを返す", () => {
    expect(isValidEmail("user@")).toBe(false);
  });

  it("ユーザー名がないとfalseを返す", () => {
    expect(isValidEmail("@example.com")).toBe(false);
  });

  it("空白を含むとfalseを返す", () => {
    expect(isValidEmail("user @example.com")).toBe(false);
  });
});

describe("isWithinLength", () => {
  it("最大長以下の文字列はtrueを返す", () => {
    expect(isWithinLength("abc", 5)).toBe(true);
  });

  it("最大長ちょうどの文字列はtrueを返す", () => {
    expect(isWithinLength("abcde", 5)).toBe(true);
  });

  it("最大長を超える文字列はfalseを返す", () => {
    expect(isWithinLength("abcdef", 5)).toBe(false);
  });

  it("空文字列はtrueを返す", () => {
    expect(isWithinLength("", 5)).toBe(true);
  });
});
