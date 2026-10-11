param(
    [int]$Days = 30,
    [string]$Notes = "كود تم توليده بواسطة الأدمن"
)

$SupabaseUrl = "https://xalmbtjmxjgombbortlz.supabase.co"
$AnonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhhbG1idGpteGpnb21iYm9ydGx6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTE1NTE3MDAsImV4cCI6MjEwNzEyNzcwMH0.RSRxfLXSv_31k8ZhUQJvp-tJZLURMqllbZAqMRdVB0E"

$headers = @{
    "apikey" = $AnonKey
    "Authorization" = "Bearer $AnonKey"
    "Content-Type" = "application/json"
}

$body = @{
    "p_days" = $Days
    "p_notes" = $Notes
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$SupabaseUrl/rest/v1/rpc/admin_generate_license" -Method Post -Headers $headers -Body $body
    if ($response.success) {
        Write-Host ""
        Write-Host "=======================================================" -ForegroundColor Cyan
        Write-Host "       🎉 تم توليد كود اشتراك جديد بنجاح!            " -ForegroundColor Green
        Write-Host "=======================================================" -ForegroundColor Cyan
        Write-Host "🔑 كود التفعيل: $($response.license_key)" -ForegroundColor Yellow
        Write-Host "⏱️ مدة الاشتراك: $($response.duration_days) يوماً" -ForegroundColor White
        Write-Host "📝 الملاحظات:   $($response.notes)" -ForegroundColor White
        Write-Host "📅 تاريخ الإنشاء: $($response.created_at)" -ForegroundColor DarkGray
        Write-Host "=======================================================" -ForegroundColor Cyan
        Write-Host "👉 انسخ الكود وأرسله للعميل مباشرة: $($response.license_key)`n" -ForegroundColor Green
    } else {
        Write-Host "❌ فشل توليد الكود" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ خطأ أثناء الاتصال بالسحابة: $_" -ForegroundColor Red
}
