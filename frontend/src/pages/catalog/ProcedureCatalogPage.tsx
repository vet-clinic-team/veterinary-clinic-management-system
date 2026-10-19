
import { useEffect, useState } from "react";
import toast from "react-hot-toast";

import DashboardLayout from "../../components/layout/DashboardLayout";
import PageContainer from "../../components/layout/PageContainer";
import Modal from "../../components/ui/Modal";

import {
  getProceduresBySpecies,
  createProcedure,
  type ProcedureCatalogItem,
  type CreateProcedureRequest,
} from "../../services/catalogService";
import {
  getFollowUpRules,
  createFollowUpRule,
  updateFollowUpRule,
  type FollowUpRule,
  type CreateFollowUpRuleRequest,
} from "../../services/followUpRuleService";

function ProcedureCatalogPage() {
  const [species, setSpecies] = useState("CAT");

  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [ruleName, setRuleName] = useState("");
const [ruleProcedure, setRuleProcedure] = useState("");

const [repeatValue, setRepeatValue] = useState("");
const [repeatUnit, setRepeatUnit] = useState<
  "DAY" | "WEEK" | "MONTH" | "YEAR"
>("MONTH");

const [doseSteps, setDoseSteps] = useState<
  {
    intervalValue: number;
    intervalUnit: "DAY" | "WEEK" | "MONTH" | "YEAR";
  }[]
>([]);
  const [isAddRuleModalOpen, setIsAddRuleModalOpen] = useState(false);

  const [procedures, setProcedures] =
    useState<ProcedureCatalogItem[]>([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [savingRule, setSavingRule] = useState(false);
  const [editingRule, setEditingRule] = useState<FollowUpRule | null>(null);

  const [form, setForm] = useState<CreateProcedureRequest>({
    name: "",
    code: "",
    species: "CAT",
    category: "VACCINATION",
  });
  const [rules, setRules] = useState<FollowUpRule[]>([]);
const [rulesLoading, setRulesLoading] = useState(true);
const [rulesError, setRulesError] = useState("");

useEffect(() => {
  let cancelled = false;
  



  const loadRules = async () => {
    setRulesLoading(true);
    setRulesError("");

    try {
      const data = await getFollowUpRules(species);

      if (!cancelled) {
        setRules(Array.isArray(data) ? data : []);
      }
    } catch (error) {
      console.error(error);

      if (!cancelled) {
        setRulesError("Failed to load follow-up rules.");
      }
    } finally {
      if (!cancelled) {
        setRulesLoading(false);
      }
    }
  };

  loadRules();

  return () => {
    cancelled = true;
  };
}, [species]);

  useEffect(() => {
    let cancelled = false;

    const loadProcedures = async () => {
      setLoading(true);
      setError("");

      try {
        const data = await getProceduresBySpecies(species);

        if (!cancelled) {
          setProcedures(data);
        }
      } catch (error) {
        console.error(error);

        if (!cancelled) {
          setError("Failed to load procedures.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    loadProcedures();

    return () => {
      cancelled = true;
    };
  }, [species]);
  const openEditRuleModal = (rule: FollowUpRule) => {
  setEditingRule(rule);
  setRuleName(rule.name);
  setRuleProcedure(rule.procedureType);

  setRepeatValue(
    rule.repeatIntervalValue !== null
      ? String(rule.repeatIntervalValue)
      : ""
  );

  setRepeatUnit(rule.repeatIntervalUnit ?? "MONTH");

  setDoseSteps(
    rule.steps.map((step) => ({
      intervalValue: step.intervalValue,
      intervalUnit: step.intervalUnit,
    }))
  );
};
  const openAddRuleModal = () => {
  setRuleName("");
  setRuleProcedure("");
  setRepeatValue("");
  setRepeatUnit("MONTH");
  setDoseSteps([]);

  setIsAddRuleModalOpen(true);
};

  const openAddModal = () => {
    setForm({
      name: "",
      code: "",
      species,
      category: "VACCINATION",
    });

    setIsAddModalOpen(true);
  };

  const closeAddModal = () => {
    if (saving) return;

    setIsAddModalOpen(false);
  };
  const handleSaveRule = async () => {
  if (savingRule) return;

  const payload: CreateFollowUpRuleRequest = {
    species,
    procedureType: ruleProcedure,
    name: ruleName.trim(),
    steps: doseSteps.map((step, index) => ({
      stepNumber: index + 1,
      intervalValue: step.intervalValue,
      intervalUnit: step.intervalUnit,
    })),
    repeatIntervalValue: repeatValue
      ? Number(repeatValue)
      : null,
    repeatIntervalUnit: repeatValue ? repeatUnit : null,
  };

  if (!payload.name || !payload.procedureType) {
    toast.error("Please enter a rule name and select a procedure.");
    return;
  }

  if (
    payload.steps.some(
      (step) => !Number.isInteger(step.intervalValue) ||
        step.intervalValue < 1
    ) ||
    (payload.repeatIntervalValue !== null &&
      (!Number.isInteger(payload.repeatIntervalValue) ||
        payload.repeatIntervalValue < 1))
  ) {
    toast.error("Intervals must be positive whole numbers.");
    return;
  }

  if (
    payload.steps.length === 0 &&
    payload.repeatIntervalValue === null
  ) {
    toast.error("Add at least one dose step or repeat interval.");
    return;
  }

 setSavingRule(true);

try {
  if (editingRule) {
    await updateFollowUpRule(editingRule.id, {
  name: payload.name,
  active: editingRule.active,
  steps: payload.steps,
  repeatIntervalValue: payload.repeatIntervalValue,
  repeatIntervalUnit: payload.repeatIntervalUnit,
});
    toast.success("Follow-up rule updated successfully.");
    setEditingRule(null);
  } else {
    await createFollowUpRule(payload);
    toast.success("Follow-up rule created successfully.");
    setIsAddRuleModalOpen(false);
  }
} catch (error) {
  console.error(error);
  toast.error("Failed to create follow-up rule.");
  setSavingRule(false);
  return;
}

try {
  setRulesLoading(true);

  const updatedRules = await getFollowUpRules(species);
  setRules(updatedRules);
  setRulesError("");
} catch (error) {
  console.error(error);
  setRulesError("Failed to refresh follow-up rules.");
} finally {
  setRulesLoading(false);
  setSavingRule(false);
}
};

  const handleSaveProcedure = async (
    event: React.FormEvent<HTMLFormElement>
  ) => {
    event.preventDefault();

    if (saving) return;

    const payload: CreateProcedureRequest = {
      name: form.name.trim(),
      code: form.code.trim().toUpperCase(),
      species: form.species,
      category: form.category,
    };

    if (!payload.name || !payload.code) {
      toast.error("Please fill in all required fields.");
      return;
    }

    setSaving(true);

    try {
      await createProcedure(payload);

      toast.success("Procedure created successfully.");

      setIsAddModalOpen(false);

      if (species !== payload.species) {
        setSpecies(payload.species);
      } else {
        setLoading(true);
        setError("");

        try {
          const data = await getProceduresBySpecies(species);
          setProcedures(data);
        } catch (error) {
          console.error(error);
          setError("Failed to refresh procedures.");
        } finally {
          setLoading(false);
        }
      }
    } catch (error: unknown) {
      console.error(error);

      const message =
        (
          error as {
            response?: {
              data?: {
                message?: string;
              };
            };
          }
        )?.response?.data?.message ??
        "Failed to create procedure.";

      toast.error(message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <DashboardLayout>
      <PageContainer>
        {/* Page Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-slate-900">
            Procedure Catalog
          </h1>

          <p className="mt-2 text-slate-500">
            Manage vaccines, treatments, and follow-up rules.
          </p>
        </div>

        {/* Procedure List */}
        <div className="rounded-2xl border border-slate-200 bg-white p-6">
          <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
            <h2 className="text-lg font-semibold text-slate-900">
              Available Procedures
            </h2>

            <div className="flex items-center gap-3">
              <select
                value={species}
                onChange={(event) =>
                  setSpecies(event.target.value)
                }
                className="rounded-xl border border-slate-300 bg-white px-4 py-2.5"
              >
                <option value="CAT">Cat</option>
                <option value="DOG">Dog</option>
              </select>

              <button
                type="button"
                onClick={openAddModal}
                className="rounded-xl bg-blue-600 px-5 py-2.5 font-medium text-white transition hover:bg-blue-700"
              >
                + Add Procedure
              </button>
            </div>
          </div>

          {loading ? (
            <p className="text-slate-500">
              Loading procedures...
            </p>
          ) : error ? (
            <p className="text-red-600">{error}</p>
          ) : procedures.length === 0 ? (
            <p className="text-slate-500">
              No procedures found for this species.
            </p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="border-b border-slate-200 text-slate-500">
                  <tr>
                    <th className="px-4 py-3">Name</th>
                    <th className="px-4 py-3">Code</th>
                    <th className="px-4 py-3">Category</th>
                    <th className="px-4 py-3">Status</th>
                  </tr>
                </thead>

                <tbody>
                  {procedures.map((procedure) => (
                    <tr
                      key={procedure.id}
                      className="border-b border-slate-100"
                    >
                      <td className="px-4 py-4 font-medium text-slate-900">
                        {procedure.name}
                      </td>

                      <td className="px-4 py-4 text-slate-600">
                        {procedure.code}
                      </td>

                      <td className="px-4 py-4 text-slate-600">
                        {procedure.category}
                      </td>

                      <td className="px-4 py-4 text-slate-600">
                        {procedure.active
                          ? "Active"
                          : "Inactive"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
        {/* Follow-up Rules List */}
<div className="mt-6 rounded-2xl border border-slate-200 bg-white p-6">
  <div className="mb-5 flex items-center justify-between gap-4">
  <h2 className="text-lg font-semibold text-slate-900">
    Follow-up Rules
  </h2>

  <button
  type="button"
  onClick={openAddRuleModal}
  className="rounded-xl bg-blue-600 px-5 py-2.5 font-medium text-white hover:bg-blue-700"
>
  + Add Rule
</button>
</div>

  {rulesLoading ? (
    <p className="text-slate-500">Loading rules...</p>
  ) : rulesError ? (
    <p className="text-red-600">{rulesError}</p>
  ) : rules.length === 0 ? (
    <p className="text-slate-500">
      No follow-up rules found for this species.
    </p>
  ) : (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-sm">
        <thead className="border-b border-slate-200 text-slate-500">
          <tr>
            <th className="px-4 py-3">Rule Name</th>
            <th className="px-4 py-3">Procedure</th>
            <th className="px-4 py-3">Dose Steps</th>
            <th className="px-4 py-3">Repeat Interval</th>
            <th className="px-4 py-3">Status</th>
            <th className="px-4 py-3">Actions</th>
          </tr>
        </thead>

        <tbody>
          {rules.map((rule) => (
            <tr
              key={rule.id}
              className="border-b border-slate-100"
            >
              <td className="px-4 py-4 font-medium text-slate-900">
                {rule.name}
              </td>

              <td className="px-4 py-4 text-slate-600">
                {rule.procedureType}
              </td>

              <td className="px-4 py-4 text-slate-600">
                {rule.steps.length > 0
                  ? rule.steps
                      .map(
                        (step) =>
                          `${step.stepNumber}: ${step.intervalValue} ${step.intervalUnit}`
                      )
                      .join(", ")
                  : "None"}
              </td>

              <td className="px-4 py-4 text-slate-600">
                {rule.repeatAfterLastStep &&
                rule.repeatIntervalValue &&
                rule.repeatIntervalUnit
                  ? `${rule.repeatIntervalValue} ${rule.repeatIntervalUnit}`
                  : "None"}
              </td>

              <td className="px-4 py-4 text-slate-600">
                {rule.active ? "Active" : "Inactive"}
              </td>
              <td className="px-4 py-4">
  <button
  type="button"
  onClick={() => openEditRuleModal(rule)}
  className="font-medium text-blue-600 hover:text-blue-800"
>
  Edit
</button>
</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )}
</div>

{/* Add / Edit Follow-up Rule Modal */}
<Modal
  open={isAddRuleModalOpen || editingRule !== null}
  onClose={() => {
    if (savingRule) return;
    setIsAddRuleModalOpen(false);
    setEditingRule(null);
  }}
  title={editingRule ? "Edit Follow-up Rule" : "Add Follow-up Rule"}
>
  <p className="text-sm text-slate-500">
    Define dose steps and repeat intervals for a procedure.
  </p>

  {/* Rule Name and Procedure */}
  <div className="mt-5 grid grid-cols-1 gap-4 md:grid-cols-2">
    <div>
      <label className="mb-2 block text-sm font-medium">
        Rule Name
      </label>
      <input
        type="text"
        value={ruleName}
        onChange={(event) => setRuleName(event.target.value)}
        placeholder="e.g. Cat Vaccination Schedule"
        className="w-full rounded-xl border border-slate-300 px-4 py-3"
      />
    </div>

    <div>
      <label className="mb-2 block text-sm font-medium">
        Procedure
      </label>
      <select
        value={ruleProcedure}
        disabled={editingRule !== null}
        onChange={(event) => setRuleProcedure(event.target.value)}
        className="w-full rounded-xl border border-slate-300 bg-white px-4 py-3"
      >
        <option value="">Select procedure</option>
        {procedures
          .filter(
  (procedure) =>
    procedure.active &&
    (
      editingRule?.procedureType === procedure.code ||
      !rules.some(
        (rule) => rule.procedureType === procedure.code
      )
    )
)
          .map((procedure) => (
            <option
              key={procedure.id}
              value={procedure.code}
            >
              {procedure.name}
            </option>
          ))}
      </select>
    </div>
  </div>

  {/* Repeat Interval */}
  <div className="mt-5 grid grid-cols-1 gap-4 md:grid-cols-2">
    <div>
      <label className="mb-2 block text-sm font-medium">
        Repeat Interval
      </label>
      <input
        type="number"
        min="1"
        value={repeatValue}
        onChange={(event) => setRepeatValue(event.target.value)}
        placeholder="e.g. 3"
        className="w-full rounded-xl border border-slate-300 px-4 py-3"
      />
    </div>

    <div>
      <label className="mb-2 block text-sm font-medium">
        Interval Unit
      </label>
      <select
        value={repeatUnit}
        onChange={(event) =>
          setRepeatUnit(
            event.target.value as
              | "DAY"
              | "WEEK"
              | "MONTH"
              | "YEAR"
          )
        }
        className="w-full rounded-xl border border-slate-300 bg-white px-4 py-3"
      >
        <option value="DAY">Day</option>
        <option value="WEEK">Week</option>
        <option value="MONTH">Month</option>
        <option value="YEAR">Year</option>
      </select>
    </div>
  </div>

  {/* Dose Steps */}
  <div className="mt-5">
    <div className="mb-3 flex items-center justify-between">
      <label className="text-sm font-medium">
        Dose Steps
      </label>

      <button
        type="button"
        onClick={() =>
          setDoseSteps([
            ...doseSteps,
            { intervalValue: 1, intervalUnit: "MONTH" },
          ])
        }
        className="rounded-lg border border-blue-600 px-3 py-2 text-sm font-medium text-blue-600 hover:bg-blue-50"
      >
        + Add Step
      </button>
    </div>

    {doseSteps.map((step, index) => (
      <div
        key={index}
        className="mb-3 flex flex-wrap items-center gap-3"
      >
        <span className="text-sm text-slate-500">
          Step {index + 1}
        </span>

        <input
          type="number"
          min="1"
          value={step.intervalValue}
          onChange={(event) =>
            setDoseSteps(
              doseSteps.map((item, i) =>
                i === index
                  ? {
                      ...item,
                      intervalValue: Number(event.target.value),
                    }
                  : item
              )
            )
          }
          className="w-24 rounded-xl border border-slate-300 px-3 py-2"
        />

        <select
          value={step.intervalUnit}
          onChange={(event) =>
            setDoseSteps(
              doseSteps.map((item, i) =>
                i === index
                  ? {
                      ...item,
                      intervalUnit: event.target.value as
                        | "DAY"
                        | "WEEK"
                        | "MONTH"
                        | "YEAR",
                    }
                  : item
              )
            )
          }
          className="rounded-xl border border-slate-300 bg-white px-3 py-2"
        >
          <option value="DAY">Day</option>
          <option value="WEEK">Week</option>
          <option value="MONTH">Month</option>
          <option value="YEAR">Year</option>
        </select>

        <button
          type="button"
          onClick={() =>
            setDoseSteps(
              doseSteps.filter((_, i) => i !== index)
            )
          }
          className="text-sm font-medium text-red-600"
        >
          Remove
        </button>
      </div>
    ))}
  </div>

  {/* Actions */}
  <div className="mt-6 flex justify-end gap-3">
    <button
      type="button"
      onClick={() => {
  if (savingRule) return;
  setIsAddRuleModalOpen(false);
  setEditingRule(null);
  
}}
disabled={savingRule}
      className="rounded-xl border border-slate-300 px-5 py-2.5 font-medium text-slate-700 hover:bg-slate-100"
    >
      Cancel
    </button>

    <button
  type="button"
  onClick={handleSaveRule}
  disabled={savingRule}
  className="rounded-xl bg-blue-600 px-5 py-2.5 font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
>
  {savingRule
  ? "Saving..."
  : editingRule
    ? "Save Changes"
    : "Save Rule"}
</button>
  </div>
</Modal>

        {/* Add Procedure Modal */}
        <Modal
          open={isAddModalOpen}
          onClose={closeAddModal}
          title="Add Procedure"
        >
          <form
            onSubmit={handleSaveProcedure}
            className="space-y-5"
          >
            <p className="text-sm text-slate-500">
              Define a new vaccine or treatment for your clinic.
            </p>

            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              {/* Procedure Name */}
              <div>
                <label className="mb-2 block text-sm font-medium">
                  Procedure Name
                </label>

                <input
                  type="text"
                  required
                  maxLength={150}
                  value={form.name}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      name: event.target.value,
                    })
                  }
                  placeholder="e.g. Canine Vaccine"
                  className="w-full rounded-xl border border-slate-300 px-4 py-3"
                />
              </div>

              {/* Procedure Code */}
              <div>
                <label className="mb-2 block text-sm font-medium">
                  Procedure Code
                </label>

                <input
                  type="text"
                  required
                  value={form.code}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      code: event.target.value,
                    })
                  }
                  placeholder="e.g. CANINE_VACCINE"
                  className="w-full rounded-xl border border-slate-300 px-4 py-3"
                />
              </div>

              {/* Species */}
              <div>
                <label className="mb-2 block text-sm font-medium">
                  Species
                </label>

                <select
                  value={form.species}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      species: event.target.value,
                    })
                  }
                  className="w-full rounded-xl border border-slate-300 bg-white px-4 py-3"
                >
                  <option value="CAT">Cat</option>
                  <option value="DOG">Dog</option>
                </select>
              </div>

              {/* Category */}
              <div>
                <label className="mb-2 block text-sm font-medium">
                  Category
                </label>

                <select
                  value={form.category}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      category: event.target.value,
                    })
                  }
                  className="w-full rounded-xl border border-slate-300 bg-white px-4 py-3"
                >
                  <option value="VACCINATION">
                    Vaccination
                  </option>

                  <option value="PARASITE_TREATMENT">
                    Parasite Treatment
                  </option>
                </select>
              </div>
            </div>

            {/* Actions */}
            <div className="flex justify-end gap-3 pt-3">
              <button
                type="button"
                onClick={closeAddModal}
                disabled={saving}
                className="rounded-xl border border-slate-300 px-5 py-2.5 font-medium text-slate-700 hover:bg-slate-100 disabled:opacity-50"
              >
                Cancel
              </button>

              <button
                type="submit"
                disabled={saving}
                className="rounded-xl bg-blue-600 px-5 py-2.5 font-medium text-white hover:bg-blue-700 disabled:opacity-50"
              >
                {saving ? "Saving..." : "Save Procedure"}
              </button>
            </div>
          </form>
        </Modal>
      </PageContainer>
    </DashboardLayout>
  );
}

export default ProcedureCatalogPage;



