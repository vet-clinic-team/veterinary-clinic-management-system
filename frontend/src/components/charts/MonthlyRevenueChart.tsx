import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
} from "recharts";

type MonthlyRevenueChartProps = {
  data: {
    month: string;
    revenue: number;
  }[];
};

function formatMonth(month: string) {
  const date = new Date(`${month}-01`);

  return date.toLocaleDateString("en-GB", {
    month: "short",
  });
}

function MonthlyRevenueChart({
  data,
}: MonthlyRevenueChartProps) {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <BarChart
        data={data}
        margin={{
          top: 10,
          right: 15,
          left: -10,
          bottom: 10,
        }}
      >
        <CartesianGrid
          strokeDasharray="3 3"
          vertical={false}
        />

        <XAxis
          dataKey="month"
          tickFormatter={formatMonth}
          axisLine={false}
          tickLine={false}
          tick={{
            fill: "#64748b",
            fontSize: 12,
          }}
        />

        <YAxis
          axisLine={false}
          tickLine={false}
          allowDecimals={false}
          tick={{
            fill: "#64748b",
            fontSize: 12,
          }}
        />

        <Tooltip
          formatter={(value) =>
  [
    typeof value === "number"
      ? value.toFixed(2)
      : "0.00",
    "Revenue",
  ]
}
          contentStyle={{
            borderRadius: "12px",
            border: "1px solid #e2e8f0",
            boxShadow:
              "0 8px 24px rgba(15,23,42,0.08)",
          }}
        />

        <Bar
          dataKey="revenue"
          radius={[8, 8, 0, 0]}
          fill="#2563EB"
        />
      </BarChart>
    </ResponsiveContainer>
  );
}

export default MonthlyRevenueChart;