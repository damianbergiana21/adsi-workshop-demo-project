import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiClientError, apiClient } from "./api-client";

describe("ApiClientError", () => {
  it("プロパティが正しくセットされる", () => {
    const error = new ApiClientError(400, "Bad Request", "入力エラー", { name: "必須です" });
    expect(error.status).toBe(400);
    expect(error.title).toBe("Bad Request");
    expect(error.detail).toBe("入力エラー");
    expect(error.errors).toEqual({ name: "必須です" });
    expect(error.name).toBe("ApiClientError");
    expect(error.message).toBe("入力エラー");
  });

  it("errorsがundefinedでも動作する", () => {
    const error = new ApiClientError(500, "Internal Server Error", "サーバーエラー");
    expect(error.errors).toBeUndefined();
  });
});

describe("apiClient", () => {
  const mockFetch = vi.fn();

  beforeEach(() => {
    vi.stubGlobal("fetch", mockFetch);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  function mockJsonResponse(status: number, body: unknown) {
    return Promise.resolve({
      ok: status >= 200 && status < 300,
      status,
      json: () => Promise.resolve(body),
    } as Response);
  }

  describe("get", () => {
    it("正常なレスポンスを返す", async () => {
      const data = { id: "1", name: "テスト" };
      mockFetch.mockReturnValue(mockJsonResponse(200, data));

      const result = await apiClient.get("/api/test");

      expect(mockFetch).toHaveBeenCalledWith("/api/test", {
        credentials: "include",
        headers: { Accept: "application/json" },
      });
      expect(result).toEqual(data);
    });

    it("エラーレスポンスでApiClientErrorをスローする", async () => {
      mockFetch.mockReturnValue(
        mockJsonResponse(404, { status: 404, title: "Not Found", detail: "見つかりません" }),
      );

      await expect(apiClient.get("/api/test")).rejects.toThrow(ApiClientError);
      await expect(apiClient.get("/api/test")).rejects.toMatchObject({
        status: 404,
        detail: "見つかりません",
      });
    });
  });

  describe("post", () => {
    it("ボディ付きでPOSTリクエストを送信する", async () => {
      mockFetch.mockReturnValue(mockJsonResponse(200, { success: true }));

      await apiClient.post("/api/test", { key: "value" });

      expect(mockFetch).toHaveBeenCalledWith("/api/test", {
        method: "POST",
        credentials: "include",
        headers: expect.objectContaining({ "Content-Type": "application/json" }),
        body: JSON.stringify({ key: "value" }),
      });
    });

    it("ボディなしでもPOSTできる", async () => {
      mockFetch.mockReturnValue(mockJsonResponse(200, { success: true }));

      await apiClient.post("/api/test");

      expect(mockFetch).toHaveBeenCalledWith("/api/test", {
        method: "POST",
        credentials: "include",
        headers: expect.objectContaining({ "Content-Type": "application/json" }),
        body: undefined,
      });
    });
  });

  describe("put", () => {
    it("PUTリクエストを送信する", async () => {
      mockFetch.mockReturnValue(mockJsonResponse(200, { updated: true }));

      await apiClient.put("/api/test/1", { name: "更新" });

      expect(mockFetch).toHaveBeenCalledWith("/api/test/1", {
        method: "PUT",
        credentials: "include",
        headers: expect.objectContaining({ "Content-Type": "application/json" }),
        body: JSON.stringify({ name: "更新" }),
      });
    });
  });

  describe("delete", () => {
    it("DELETEリクエストを送信する", async () => {
      mockFetch.mockReturnValue(
        Promise.resolve({ ok: true, status: 204, json: () => Promise.resolve(undefined) }),
      );

      await apiClient.delete("/api/test/1");

      expect(mockFetch).toHaveBeenCalledWith("/api/test/1", {
        method: "DELETE",
        credentials: "include",
        headers: expect.objectContaining({ Accept: "application/json" }),
      });
    });
  });

  describe("204レスポンス", () => {
    it("undefinedを返す", async () => {
      mockFetch.mockReturnValue(
        Promise.resolve({ ok: true, status: 204, json: () => Promise.resolve(undefined) }),
      );

      const result = await apiClient.post("/api/test");
      expect(result).toBeUndefined();
    });
  });

  describe("401レスポンス", () => {
    it("ApiClientErrorをスローする", async () => {
      mockFetch.mockReturnValue(
        Promise.resolve({ ok: false, status: 401, json: () => Promise.resolve({}) }),
      );

      await expect(apiClient.get("/api/test")).rejects.toThrow(ApiClientError);
      await expect(apiClient.get("/api/test")).rejects.toMatchObject({
        status: 401,
        detail: "認証が必要です",
      });
    });
  });
});
