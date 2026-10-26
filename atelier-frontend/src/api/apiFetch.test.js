import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { API_URL } from "../config";
import { apiFetch } from "./apiFetch";

describe("apiFetch", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    fetchMock.mockReset();
  });

  it("sends safe requests without fetching a CSRF token", async () => {
    fetchMock.mockResolvedValueOnce({ ok: true, status: 200 });

    await apiFetch("/api/me");

    expect(fetchMock).toHaveBeenCalledOnce();
    expect(fetchMock).toHaveBeenCalledWith(
      `${API_URL}/api/me`,
      expect.objectContaining({ credentials: "include" }),
    );
  });

  it("fetches a CSRF token and adds its header to unsafe requests", async () => {
    fetchMock
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ headerName: "X-CSRF-TOKEN", token: "csrf-value" }),
      })
      .mockResolvedValueOnce({ ok: true, status: 200 });

    await apiFetch("/logout", { method: "POST" });

    expect(fetchMock).toHaveBeenNthCalledWith(1, `${API_URL}/csrf`, {
      credentials: "include",
    });
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      `${API_URL}/logout`,
      expect.objectContaining({
        credentials: "include",
        headers: expect.objectContaining({ "X-CSRF-TOKEN": "csrf-value" }),
      }),
    );
  });

  it("does not send an unsafe request when CSRF bootstrap fails", async () => {
    fetchMock.mockResolvedValueOnce({ ok: false, status: 401 });

    await expect(apiFetch("/logout", { method: "POST" }))
      .rejects.toThrow("Failed to fetch CSRF token (401)");

    expect(fetchMock).toHaveBeenCalledOnce();
  });
});
