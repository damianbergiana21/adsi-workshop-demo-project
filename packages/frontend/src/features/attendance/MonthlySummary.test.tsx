import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { MonthlySummary } from "./MonthlySummary";

const mockSummary = {
  workDays: 20,
  totalWorkMinutes: 9600,
  totalOvertimeMinutes: 120,
  absentDays: 1,
};

describe("MonthlySummary", () => {
  it("サマリーデータを正しく表示する", () => {
    render(<MonthlySummary summary={mockSummary} isLoading={false} />);

    expect(screen.getByText("出勤日数")).toBeInTheDocument();
    expect(screen.getByText("20日")).toBeInTheDocument();
    expect(screen.getByText("総勤務時間")).toBeInTheDocument();
    expect(screen.getByText("160h")).toBeInTheDocument();
    expect(screen.getByText("総残業時間")).toBeInTheDocument();
    expect(screen.getByText("2h")).toBeInTheDocument();
    expect(screen.getByText("欠勤日数")).toBeInTheDocument();
    expect(screen.getByText("1日")).toBeInTheDocument();
  });

  it("ローディング中はスケルトンを表示する", () => {
    const { container } = render(<MonthlySummary summary={undefined} isLoading={true} />);
    expect(container.querySelectorAll("[data-slot='skeleton']").length).toBeGreaterThan(0);
  });

  it("summaryがundefinedかつロード完了ならnullを返す", () => {
    const { container } = render(<MonthlySummary summary={undefined} isLoading={false} />);
    expect(container.innerHTML).toBe("");
  });
});
