package com.example.bataanexplorer

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth // <-- Idinagdag ang import na ito para sa Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

object SupabaseClientProvider {
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = "https://pzqdtkcpfcyqwkbzinks.supabase.co",
            supabaseKey = "sb_publishable_DIPEWag8aR7Y7xm2Y9P7BA_slrq1YJe"
        ) {
            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            })
            install(Auth)
            install(Postgrest)
        }
    }
}