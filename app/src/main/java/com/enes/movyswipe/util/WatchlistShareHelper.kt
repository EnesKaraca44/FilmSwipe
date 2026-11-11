package com.enes.movyswipe.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import com.enes.movyswipe.BuildConfig
import com.enes.movyswipe.data.MovieFormat
import com.enes.movyswipe.data.WatchlistMovie
import java.io.File

/**
 * Listem > "Listeyi Paylaş": metin + uygulama linki ya da Instagram/WhatsApp
 * hikayesine uygun 1080x1920 bir görsel üretir.
 */
object WatchlistShareHelper {

    val storeLink: String
        get() = "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"

    fun buildShareText(movies: List<WatchlistMovie>): String = buildString {
        append("🎬 FilmSwipe İzleme Listem (${movies.size} film)\n\n")
        movies.forEachIndexed { index, movie ->
            val year = MovieFormat.releaseYear(movie.releaseDate)?.let { " ($it)" }.orEmpty()
            append("${index + 1}. ${movie.title}$year\n")
        }
        append("\nBirlikte film seçelim! FilmSwipe'ı indir: $storeLink")
    }

    /** Hikaye görselini önbelleğe yazar ve paylaşılabilir content:// adresini döndürür. Arka planda çağır. */
    fun createStoryImage(context: Context, movies: List<WatchlistMovie>): Uri {
        val bitmap = renderStory(movies)
        val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
        val file = File(dir, "filmswipe_izleme_listem.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private const val SHARE_DIR = "shared"
    private const val WIDTH = 1080
    private const val HEIGHT = 1920
    private const val MAX_ROWS = 7

    private fun renderStory(movies: List<WatchlistMovie>): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = WIDTH.toFloat()
        val h = HEIGHT.toFloat()

        // Arka plan + mor parıltı
        canvas.drawRect(0f, 0f, w, h, Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, h, intArrayOf(0xFF0A0B14.toInt(), 0xFF16123A.toInt(), 0xFF0A0B14.toInt()), null, Shader.TileMode.CLAMP)
        })
        canvas.drawCircle(w * 0.85f, h * 0.12f, 520f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(w * 0.85f, h * 0.12f, 520f, 0x557C3AED, 0x007C3AED, Shader.TileMode.CLAMP)
        })

        val bold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val margin = 80f

        // Başlık
        val titlePaint = textPaint(0xFFF5F3FF.toInt(), 92f, bold)
        canvas.drawText("İzleme Listem", margin, 280f, titlePaint)
        canvas.drawCircle(margin + titlePaint.measureText("İzleme Listem") + 34f, 252f, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFA855F7.toInt() })
        canvas.drawText("${movies.size} film • FilmSwipe Ortak Arşiv", margin, 350f, textPaint(0xFF9A9CB3.toInt(), 40f))

        // Film satırları
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF15162A.toInt() }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF2B2E49.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f
        }
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val numberPaint = textPaint(0xFFFFFFFF.toInt(), 44f, bold).apply { textAlign = Paint.Align.CENTER }
        val rowTitlePaint = textPaint(0xFFF5F3FF.toInt(), 46f, bold)
        val metaPaint = textPaint(0xFF9A9CB3.toInt(), 34f)
        val ratingPaint = textPaint(0xFFF7BB18.toInt(), 34f, bold)

        val rowHeight = 140f
        val rowGap = 22f
        var top = 440f
        movies.take(MAX_ROWS).forEachIndexed { index, movie ->
            val rect = RectF(margin, top, w - margin, top + rowHeight)
            canvas.drawRoundRect(rect, 36f, 36f, cardPaint)
            canvas.drawRoundRect(rect, 36f, 36f, borderPaint)

            val badgeCx = margin + 80f
            val badgeCy = top + rowHeight / 2
            badgePaint.shader = LinearGradient(badgeCx - 46f, 0f, badgeCx + 46f, 0f, 0xFF6D28D9.toInt(), 0xFFA855F7.toInt(), Shader.TileMode.CLAMP)
            canvas.drawRoundRect(RectF(badgeCx - 46f, badgeCy - 46f, badgeCx + 46f, badgeCy + 46f), 26f, 26f, badgePaint)
            canvas.drawText("${index + 1}", badgeCx, badgeCy + 16f, numberPaint)

            val textLeft = margin + 160f
            val textWidth = w - margin - 40f - textLeft
            val title = TextUtils.ellipsize(movie.title, rowTitlePaint, textWidth, TextUtils.TruncateAt.END).toString()
            canvas.drawText(title, textLeft, top + 64f, rowTitlePaint)

            val year = MovieFormat.releaseYear(movie.releaseDate) ?: "—"
            canvas.drawText("$year  •", textLeft, top + 112f, metaPaint)
            canvas.drawText("★ ${MovieFormat.rating(movie.voteAverage)}", textLeft + metaPaint.measureText("$year  •  "), top + 112f, ratingPaint)

            top += rowHeight + rowGap
        }

        val remaining = movies.size - MAX_ROWS
        if (remaining > 0) {
            canvas.drawText("+$remaining film daha", w / 2, top + 50f, textPaint(0xFFA78BFA.toInt(), 40f, bold).apply { textAlign = Paint.Align.CENTER })
        }

        // Alt bilgi
        canvas.drawRect(margin, h - 230f, w - margin, h - 227f, Paint().apply { color = 0xFF23253A.toInt() })
        canvas.drawText("FilmSwipe ile birlikte film seçelim!", w / 2, h - 150f, textPaint(0xFFF5F3FF.toInt(), 44f, bold).apply { textAlign = Paint.Align.CENTER })
        canvas.drawText("Google Play'de \"FilmSwipe\" ara", w / 2, h - 90f, textPaint(0xFFA78BFA.toInt(), 36f).apply { textAlign = Paint.Align.CENTER })

        return bitmap
    }

    private fun textPaint(color: Int, size: Float, typeface: Typeface = Typeface.DEFAULT) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            this.typeface = typeface
        }
}
