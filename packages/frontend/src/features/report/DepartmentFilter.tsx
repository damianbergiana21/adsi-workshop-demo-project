"use client";

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useDepartments } from "@/features/department/useDepartments";

interface DepartmentFilterProps {
  value: string;
  onChange: (departmentId: string) => void;
}

export function DepartmentFilter({ value, onChange }: DepartmentFilterProps) {
  const { data: departments = [] } = useDepartments();

  const handleChange = (newValue: string | null) => {
    onChange(newValue ?? "all");
  };

  return (
    <Select value={value} onValueChange={handleChange}>
      <SelectTrigger>
        <SelectValue placeholder="全部署">
          {(v: string | null) => {
            if (!v || v === "all") return "全部署";
            const dept = departments.find((d) => d.id === v);
            return dept?.name ?? v;
          }}
        </SelectValue>
      </SelectTrigger>
      <SelectContent>
        <SelectItem value="all">全部署</SelectItem>
        {departments.map((dept) => (
          <SelectItem key={dept.id} value={dept.id}>
            {dept.name}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
