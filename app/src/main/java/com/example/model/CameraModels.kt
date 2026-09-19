package com.example.model

import android.net.Uri

data class DeviceDetectionInfo(
    val isS24Ultra: Boolean,
    val model: String,
    val manufacturer: String,
    val deviceCode: String,
    val sensorName: String,
    val maxHardwareMegapixels: Int,
    val supportsRaw: Boolean,
    val supportsUltraHighRes: Boolean,
    val hardwareLevelName: String
)

enum class CameraLens(
    val id: String,
    val label: String,
    val zoomFactor: Float,
    val description: String,
    val focalLength: String,
    val aperture: String,
    val defaultMegapixels: Int
) {
    ULTRA_WIDE("uw", "0.6x", 0.6f, "Ultra Wide", "13mm", "f/2.2", 12),
    MAIN_WIDE("main", "1.0x", 1.0f, "Principal ISOCELL HP2 (200MP)", "24mm", "f/1.7", 200),
    TELE_3X("tele3", "3.0x", 3.0f, "Teleobjetiva 3x", "67mm", "f/2.4", 10),
    PERISCOPE_5X("tele5", "5.0x", 5.0f, "Periscópio 5x Quad Tele", "111mm", "f/3.4", 50),
    ZOOM_10X("zoom10", "10x", 10.0f, "Super Zoom 10x (200MP)", "240mm", "f/1.7", 200)
}

enum class CaptureResolution(
    val label: String,
    val width: Int,
    val height: Int,
    val description: String,
    val binningMode: String
) {
    RES_12MP("12 MP", 4000, 3000, "12 MP (4000x3000)", "Binning Tetra²pixel 16-em-1 (Alta Sensibilidade)"),
    RES_50MP("50 MP", 8160, 6120, "50 MP (8160x6120)", "Binning Tetra²pixel 4-em-1 (Equilíbrio Detalhe/Ruído)"),
    RES_200MP("200 MP", 16320, 12240, "200 MP (16320x12240)", "Sensor Nativo ISOCELL HP2 Re-mosaic Completo")
}

enum class CaptureFormat(val label: String, val extension: String, val description: String) {
    JPEG("JPEG", ".jpg", "Processamento de Imagem 8-bit com perfil de cor"),
    RAW_DNG("RAW (DNG)", ".dng", "Dados brutos do sensor sem compressão (16-bit DNG)"),
    RAW_PLUS_JPEG("RAW + JPEG", ".dng/.jpg", "Arquivo DNG nativo + JPEG calibrado")
}

enum class ShootingMode(val code: String, val title: String, val shortDesc: String) {
    AUTO("AUTO", "Automático Inteligente", "Câmera gerencia ISO e Obturador"),
    SHUTTER_PRIORITY("S", "Prioridade de Velocidade", "Defina o obturador, ISO é automático"),
    ISO_PRIORITY("A", "Prioridade de Abertura/ISO", "Defina o ISO, obturador é automático"),
    MANUAL("M", "PRO Manual Completo", "Controle total manual de ISO, obturador, foco e WB")
}

enum class ColorProfile(val label: String, val description: String) {
    STANDARD("STANDARD", "Processamento padrão sRGB com gama M3"),
    FLAT_LOG("FOTO LOG", "Curva logarítmica plana para pós-processamento (Lightroom/Photoshop)"),
    VIVID("VIVID", "Cores vibrantes com saturação e contraste aprimorados"),
    MONOCHROME("P&B PRO", "Preto e branco com separação tonal acentuada")
}

enum class AwbPreset(val label: String, val kelvin: Int, val description: String) {
    AUTO("Auto", 0, "Balanço Automático de Branco"),
    DAYLIGHT("Luz do Dia", 5500, "5500K - Sol pleno ao ar livre"),
    CLOUDY("Nublado", 6500, "6500K - Luz difusa sob nuvens"),
    SHADE("Sombra", 7500, "7500K - Sombra aberta sob céu azul"),
    INCANDESCENT("Tungstênio", 3200, "3200K - Lâmpadas incandescentes"),
    FLUORESCENT("Fluorescente", 4000, "4000K - Lâmpadas de escritório"),
    CUSTOM_KELVIN("Manual", 0, "Ajuste manual preciso em graus Kelvin"),
    MANUAL_2("Manual 2", 0, "Ajuste fino de Brilho, Contraste, Sombras e Exposição")
}

data class Manual2Adjustments(
    val brightness: Float = 0f,    // -100f to +100f
    val contrast: Float = 0f,      // -100f to +100f
    val shadows: Float = 0f,       // -100f to +100f
    val highlights: Float = 0f     // -100f to +100f
)

enum class DenoiseMode(val label: String, val shortLabel: String, val description: String) {
    OFF("Desativado", "OFF", "Sem redução de ruído (textura pura do sensor)"),
    STANDARD("Padrão", "STD", "Redução de ruído padrão de hardware"),
    AI_NEURAL("AI Neural (Gemini)", "AI", "Remoção avançada de ruído neural preservando nitidez de bordas")
}

enum class ActiveDial {
    NONE, ISO, SHUTTER, EV, FOCUS, WB
}

data class ShutterSpeedOption(
    val display: String,
    val nanoseconds: Long
)

data class HistogramData(
    val r: FloatArray = FloatArray(64),
    val g: FloatArray = FloatArray(64),
    val b: FloatArray = FloatArray(64),
    val luma: FloatArray = FloatArray(64),
    val highlightClippingPercent: Float = 0f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HistogramData) return false
        return r.contentEquals(other.r) &&
                g.contentEquals(other.g) &&
                b.contentEquals(other.b) &&
                luma.contentEquals(other.luma) &&
                highlightClippingPercent == other.highlightClippingPercent
    }

    override fun hashCode(): Int {
        var result = r.contentHashCode()
        result = 31 * result + g.contentHashCode()
        result = 31 * result + b.contentHashCode()
        result = 31 * result + luma.contentHashCode()
        result = 31 * result + highlightClippingPercent.hashCode()
        return result
    }
}

data class CapturedMediaInfo(
    val uri: Uri,
    val filePath: String,
    val fileName: String,
    val format: String,
    val resolution: String,
    val width: Int,
    val height: Int,
    val megapixels: Float,
    val fileSizeFormatted: String,
    val iso: Int,
    val exposureTime: String,
    val fNumber: String,
    val focalLength: String,
    val colorProfile: String,
    val isDng: Boolean,
    val timestamp: Long,
    val orientationDegrees: Int = 0,
    val denoiseMode: String = "Padrão"
)
