import { getStore } from "@netlify/blobs";

const TABLES = [
  "Exercise", "Workout", "WorkoutSet", "Program", "ProgramDay",
  "ProgramExercise", "Food", "FoodLog", "WeightEntry", "GroceryItem",
  "Routine", "RoutineExercise", "Cardio",
];

export default async (req: Request) => {
  if (req.method !== "POST") return new Response("method not allowed", { status: 405 });
  const auth = req.headers.get("authorization");
  if (!process.env.SYNC_TOKEN || auth !== `Bearer ${process.env.SYNC_TOKEN}`) {
    return new Response("unauthorized", { status: 401 });
  }

  const { since = 0, changes = {} } = await req.json();
  const store = getStore("liftlog-sync");
  const out: Record<string, unknown[]> = {};

  for (const table of TABLES) {
    const cur: Record<string, any> = (await store.get(table, { type: "json" })) ?? {};
    const incoming: any[] = changes[table] ?? [];
    let dirty = false;
    for (const row of incoming) {
      if (!row?.id) continue;
      const prev = cur[row.id];
      if (!prev || prev.updatedAt < row.updatedAt) {
        cur[row.id] = row;
        dirty = true;
      }
    }
    if (dirty) await store.setJSON(table, cur);
    const newer = Object.values(cur).filter((r: any) => r.updatedAt > since);
    if (newer.length) out[table] = newer;
  }

  return Response.json({ now: Date.now(), changes: out });
};

export const config = { path: "/api/sync" };
