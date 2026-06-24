import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { StatusBadge } from "./StatusBadge";

const configMap = {
  ACTIVE: { label: "在籍", variant: "default" as const },
  RETIRED: { label: "退職済", variant: "destructive" as const },
};

describe("StatusBadge", () => {
  it("configMapに存在するステータスのラベルを表示する", () => {
    render(<StatusBadge status="ACTIVE" configMap={configMap} />);
    expect(screen.getByText("在籍")).toBeInTheDocument();
  });

  it("configMapに存在しないステータスはステータス文字列をそのまま表示する", () => {
    render(<StatusBadge status="UNKNOWN" configMap={configMap} />);
    expect(screen.getByText("UNKNOWN")).toBeInTheDocument();
  });
});
