import { createClient } from "npm:@supabase/supabase-js@2";
// Compare device keys without comparing their
// characters using an early-exit comparison.
async function keysMatch(a, b) {
  const encoder = new TextEncoder();
  const [hashA, hashB] = await Promise.all([
    crypto.subtle.digest("SHA-256", encoder.encode(a)),
    crypto.subtle.digest("SHA-256", encoder.encode(b))
  ]);
  const bytesA = new Uint8Array(hashA);
  const bytesB = new Uint8Array(hashB);
  let difference = 0;
  for(let i = 0; i < bytesA.length; i++){
    difference |= bytesA[i] ^ bytesB[i];
  }
  return difference === 0;
}
Deno.serve(async (req)=>{
  // 1. Allow POST only.
  if (req.method !== "POST") {
    return Response.json({
      success: false,
      message: "Method not allowed"
    }, {
      status: 405
    });
  }
  // 2. Load secret configuration.
  const deviceKey = Deno.env.get("IOT_DEVICE_KEY");
  const storageId = Number(Deno.env.get("IOT_STORAGE_ID"));
  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const secretKeys = JSON.parse(Deno.env.get("SUPABASE_SECRET_KEYS") ?? "{}");
  const serverKey = secretKeys["default"] ?? Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!deviceKey || !supabaseUrl || !serverKey || !Number.isSafeInteger(storageId) || storageId <= 0) {
    return Response.json({
      success: false,
      message: "Server configuration error"
    }, {
      status: 500
    });
  }
  // 3. Authenticate ESP32 device.
  const providedKey = req.headers.get("x-device-key") ?? "";
  if (!await keysMatch(providedKey, deviceKey)) {
    return Response.json({
      success: false,
      message: "Unauthorized device"
    }, {
      status: 401
    });
  }
  // 4. Read JSON request.
  let body;
  try {
    body = await req.json();
  } catch  {
    return Response.json({
      success: false,
      message: "Invalid JSON"
    }, {
      status: 400
    });
  }
  if (!body || typeof body !== "object") {
    return Response.json({
      success: false,
      message: "Invalid request body"
    }, {
      status: 400
    });
  }
  const { reading_key, temperature } = body;
  // 5. Validate temperature and reading key.
  if (typeof reading_key !== "string" || !/^[A-Za-z0-9_-]{1,80}$/.test(reading_key) || typeof temperature !== "number" || !Number.isFinite(temperature) || temperature < -55 || temperature > 125) {
    return Response.json({
      success: false,
      message: "Invalid temperature or reading key"
    }, {
      status: 400
    });
  }
  // 6. Create server-side Supabase client.
  const supabase = createClient(supabaseUrl, serverKey);
  try {
    // 7. Execute existing PostgreSQL function.
    const { data, error } = await supabase.rpc("ingest_temperature", {
      p_storage_id: storageId,
      p_reading_key: reading_key,
      p_temperature: temperature
    });
    if (error || !data?.[0]) {
      console.error("Database operation failed:", error);
      return Response.json({
        success: false,
        message: "Unable to save reading"
      }, {
        status: 500
      });
    }
    // 8. Return result to ESP32.
    return Response.json({
      success: true,
      ...data[0]
    }, {
      status: data[0].is_duplicate ? 200 : 201
    });
  } catch (error) {
    console.error("Temperature processing failed:", error);
    return Response.json({
      success: false,
      message: "Internal server error"
    }, {
      status: 500
    });
  }
});
