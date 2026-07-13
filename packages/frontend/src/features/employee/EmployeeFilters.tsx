"use client";

import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { DepartmentSummary } from "./employee-api";

interface EmployeeFiltersProps {
  departmentId: string;
  onDepartmentIdChange: (value: string) => void;
  role: string;
  onRoleChange: (value: string) => void;
  includeRetired: boolean;
  onIncludeRetiredChange: (value: boolean) => void;
  departments: DepartmentSummary[];
}

export function EmployeeFilters({
  departmentId,
  onDepartmentIdChange,
  role,
  onRoleChange,
  includeRetired,
  onIncludeRetiredChange,
  departments,
}: EmployeeFiltersProps) {
  return (
    <div className="flex flex-wrap items-center gap-3">
      <Select
        value={departmentId || null}
        onValueChange={(value) => onDepartmentIdChange(value ?? "")}
      >
        <SelectTrigger>
          <SelectValue placeholder="部署で絞り込み">
            {(value: string | null) => {
              if (!value) return "部署で絞り込み";
              const dept = departments.find((d) => d.id === value);
              return dept?.name ?? value;
            }}
          </SelectValue>
        </SelectTrigger>
        <SelectContent>
          {departments.map((dept) => (
            <SelectItem key={dept.id} value={dept.id}>
              {dept.name}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      <Select value={role || null} onValueChange={(value) => onRoleChange(value ?? "")}>
        <SelectTrigger>
          <SelectValue placeholder="ロールで絞り込み" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="ADMIN">管理者</SelectItem>
          <SelectItem value="EMPLOYEE">一般</SelectItem>
        </SelectContent>
      </Select>

      <Button
        variant={includeRetired ? "secondary" : "outline"}
        size="sm"
        onClick={() => onIncludeRetiredChange(!includeRetired)}
      >
        {includeRetired ? "退職者を含む" : "退職者を除外"}
      </Button>
    </div>
  );
}
