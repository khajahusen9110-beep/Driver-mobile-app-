package com.example.data.api

object SupabaseConfig {
    const val SUPABASE_URL = "https://rhftkhfabmrziobhopnr.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJoZnRraGZhYm1yemlvYmhvcG5yIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTAzNDA3OTgsImV4cCI6MjEwNTkxNjc5OH0.A23YNHmOexCfWv70a1TEi62Sx6cePl6WnoK2NPMPYhQ"
    
    // REST base url
    const val REST_URL = "$SUPABASE_URL/rest/v1"
    const val AUTH_URL = "$SUPABASE_URL/auth/v1"
    const val STORAGE_URL = "$SUPABASE_URL/storage/v1"
}
