package com.example.bataanexplorer

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://pzqdtkcpfcyqwkbzinks.supabase.co",
        supabaseKey = "sb_publishable_DIPEWag8aR7Y7xm2Y9P7BA_slrq1YJe"
    ) {
        install(Auth)
        install(Postgrest)
    }
}