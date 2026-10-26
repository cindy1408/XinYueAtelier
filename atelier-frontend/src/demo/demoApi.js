const patterns = [
  { id: 1, name: "Sample bodice", /* ...your real field names */ },
];

const json = (body, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });

export async function demoFetch(path, options = {}) {
  const method = (options.method || "GET").toUpperCase();

  if (method === "GET" && path === "/api/patterns") return json(patterns);
  // ...one branch per route your app uses

  return json({ message: "Not available in demo" }, 404);
}