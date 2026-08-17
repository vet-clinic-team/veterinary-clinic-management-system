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
  const today = new Date();

  const currentYear = today.getFullYear();

  /*
   * Backend returns the last 12 months.
   * We keep the revenue values belonging to the current year
   * and create missing future months with zero revenue.
   */
  const currentYearData = data.filter((item) => {
    const [year] = item.month
      .split("-")
      .map(Number);

    return year === currentYear;
  });

  const revenueByMonth = new Map(
    currentYearData.map((item) => [
      item.month,
      item.revenue,
    ])
  );

  const chartData = Array.from(
    { length: 12 },
    (_, index) => {
      const monthNumber =
        String(index + 1).padStart(2, "0");

      const monthKey =
        `${currentYear}-${monthNumber}`;

      return {
        month: monthKey,
        revenue:
          revenueByMonth.get(monthKey) ?? 0,
      };
    }
  );

  return (
    <ResponsiveContainer
      width="100%"
      height="100%"
    >
      <BarChart
        data={chartData}
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
          formatter={(value) => [
            typeof value === "number"
              ? value.toFixed(2)
              : "0.00",
            "Revenue",
          ]}
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