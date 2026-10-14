package com.fittrack.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import com.fittrack.app.data.model.ZombieAlertLevel

/**
 * High-End Vector Animation Factory for Runner and Zombie Horde.
 * Produces polished, high-resolution, game-quality animated character sprites
 * with layered lighting, aerodynamic athletic silhouettes, terrifying monster lunges,
 * pulsing shockwave auras, and glassmorphism HUD badges.
 *
 * Utilizes LRU caching to eliminate GC pressure and prevent frame drops during 8 FPS animation loops.
 */
object MarkerAnimationFactory {

    private val runnerCache = HashMap<String, BitmapDrawable>()
    private val zombieCache = LruCache<String, BitmapDrawable>(128)

    /**
     * Generates a high-definition animated frame for the Runner character.
     */
    fun createRunnerFrameDrawable(
        context: Context,
        frame: Int,
        isMoving: Boolean
    ): BitmapDrawable {
        val key = "$frame-$isMoving"
        runnerCache[key]?.let { return it }

        val w = 130
        val h = 156
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cx = w / 2f
        val cy = 54f

        // 1. Dynamic Ground Contact Shadow (squashes and stretches with stride bounce)
        val shadowW = if (isMoving && (frame == 1 || frame == 3)) 18f else 26f
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4D000000")
            style = Paint.Style.FILL
        }
        canvas.drawOval(cx - shadowW, cy + 34f, cx + shadowW, cy + 42f, shadowPaint)

        // 2. Dual Concentric Cyan Pulse Rings
        val pulse1 = if (isMoving) 38f + (frame * 3.5f) else 38f
        val pulsePaint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00F0FF")
            alpha = (140 - frame * 30).coerceIn(20, 140)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawCircle(cx, cy, pulse1, pulsePaint1)

        val pulse2 = pulse1 - 8f
        if (pulse2 > 26f) {
            val pulsePaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#06B6D4")
                alpha = (90 - frame * 20).coerceIn(10, 90)
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawCircle(cx, cy, pulse2, pulsePaint2)
        }

        // 3. Cybernetic Hologram Base Disc
        val discRadius = 33f
        val discPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, discRadius,
                intArrayOf(Color.parseColor("#1E293B"), Color.parseColor("#0F172A")),
                floatArrayOf(0.4f, 1.0f),
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, discRadius, discPaint)

        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00F0FF")
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }
        canvas.drawCircle(cx, cy, discRadius, rimPaint)

        // Glowing tick marks at 4 cardinal notches
        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            strokeWidth = 2.5f
        }
        canvas.drawLine(cx, cy - discRadius - 2f, cx, cy - discRadius + 4f, tickPaint)
        canvas.drawLine(cx, cy + discRadius - 4f, cx, cy + discRadius + 2f, tickPaint)
        canvas.drawLine(cx - discRadius - 2f, cy, cx - discRadius + 4f, cy, tickPaint)
        canvas.drawLine(cx + discRadius - 4f, cy, cx + discRadius + 2f, cy, tickPaint)

        // 4. Athletic Speed Blur Ribbons (Trailing motion streaks)
        if (isMoving) {
            val ribbonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#00F0FF")
                strokeWidth = 3f
                strokeCap = Paint.Cap.ROUND
                alpha = if (frame % 2 == 0) 160 else 90
            }
            val bobShift = if (frame == 1 || frame == 3) -4f else 1f
            canvas.drawLine(cx - 20f, cy - 8f + bobShift, cx - 34f, cy - 8f + bobShift, ribbonPaint)
            canvas.drawLine(cx - 24f, cy + 4f + bobShift, cx - 40f, cy + 4f + bobShift, ribbonPaint)
            canvas.drawLine(cx - 18f, cy + 16f + bobShift, cx - 30f, cy + 16f + bobShift, ribbonPaint)
        }

        // 5. Stylized Athletic Runner Character
        val bobY = if (isMoving) {
            when (frame) {
                1, 3 -> -4f // high bounce airborne float
                else -> 1f  // ground impact
            }
        } else 0f

        // Head & Visor
        val headCx = cx
        val headCy = cy - 16f + bobY
        val skinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FDBA74")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(headCx, headCy, 8f, skinPaint)

        // Aerodynamic Hair & Sleek Neon Sunglasses
        val hairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
        }
        canvas.drawArc(RectF(headCx - 8f, headCy - 9f, headCx + 8f, headCy + 2f), 180f, 180f, true, hairPaint)

        val visorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00F0FF")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(headCx - 7f, headCy - 2.5f, headCx + 6f, headCy + 1.5f), 2f, 2f, visorPaint)

        // Athletic Torso (Curved aerodynamic running tank top)
        val chestY = headCy + 9f
        val waistY = chestY + 16f
        val torsoPath = Path().apply {
            moveTo(headCx - 7f, chestY)
            lineTo(headCx + 8f, chestY)
            lineTo(headCx + 5f, waistY)
            lineTo(headCx - 5f, waistY)
            close()
        }
        val jerseyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                headCx, chestY, headCx, waistY,
                Color.parseColor("#22D3EE"), Color.parseColor("#0284C7"),
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawPath(torsoPath, jerseyPaint)

        // Reflective chest stripe
        val sashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(headCx - 5f, chestY + 2f, headCx + 3f, waistY - 2f, sashPaint)

        // Muscular Running Limbs (Legs & Arms Stride Cycle)
        val legPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 5.5f
            strokeCap = Paint.Cap.ROUND
            color = Color.parseColor("#0F172A")
            style = Paint.Style.STROKE
        }
        val legStripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 1.8f
            strokeCap = Paint.Cap.ROUND
            color = Color.parseColor("#38BDF8")
            style = Paint.Style.STROKE
        }
        val armPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 4.5f
            strokeCap = Paint.Cap.ROUND
            color = Color.parseColor("#FDBA74")
            style = Paint.Style.STROKE
        }
        val shoePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FACC15") // neon racing yellow
            style = Paint.Style.FILL
        }
        val shoeSolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }

        if (isMoving) {
            when (frame) {
                0 -> {
                    // Right leg drives forward with high knee
                    canvas.drawLine(headCx + 2f, waistY, headCx + 10f, waistY + 8f, legPaint)
                    canvas.drawLine(headCx + 10f, waistY + 8f, headCx + 16f, waistY + 17f, legPaint)
                    canvas.drawLine(headCx + 10f, waistY + 8f, headCx + 16f, waistY + 17f, legStripePaint)
                    canvas.drawCircle(headCx + 16f, waistY + 17f, 3.5f, shoePaint)

                    // Left leg kicks back in powerful drive
                    canvas.drawLine(headCx - 2f, waistY, headCx - 9f, waistY + 7f, legPaint)
                    canvas.drawLine(headCx - 9f, waistY + 7f, headCx - 17f, waistY + 15f, legPaint)
                    canvas.drawLine(headCx - 9f, waistY + 7f, headCx - 17f, waistY + 15f, legStripePaint)
                    canvas.drawCircle(headCx - 17f, waistY + 15f, 3.5f, shoePaint)

                    // Arms: Left arm bent forward, Right arm swinging back
                    canvas.drawLine(headCx + 6f, chestY + 2f, headCx - 2f, chestY + 8f, armPaint)
                    canvas.drawLine(headCx - 2f, chestY + 8f, headCx - 9f, chestY + 5f, armPaint)

                    canvas.drawLine(headCx - 5f, chestY + 2f, headCx + 6f, chestY + 7f, armPaint)
                    canvas.drawLine(headCx + 6f, chestY + 7f, headCx + 14f, chestY + 4f, armPaint)
                }
                1, 3 -> {
                    // Mid-stride flight phase: knees bent, compact, aerodynamic
                    canvas.drawLine(headCx + 3f, waistY, headCx + 6f, waistY + 8f, legPaint)
                    canvas.drawLine(headCx + 6f, waistY + 8f, headCx + 7f, waistY + 16f, legPaint)
                    canvas.drawCircle(headCx + 7f, waistY + 16f, 3.5f, shoePaint)

                    canvas.drawLine(headCx - 3f, waistY, headCx - 6f, waistY + 8f, legPaint)
                    canvas.drawLine(headCx - 6f, waistY + 8f, headCx - 6f, waistY + 16f, legPaint)
                    canvas.drawCircle(headCx - 6f, waistY + 16f, 3.5f, shoePaint)

                    // Arms tucked close to chest
                    canvas.drawLine(headCx + 5f, chestY + 2f, headCx + 7f, chestY + 10f, armPaint)
                    canvas.drawLine(headCx - 5f, chestY + 2f, headCx - 7f, chestY + 10f, armPaint)
                }
                2 -> {
                    // Left leg drives forward with high knee
                    canvas.drawLine(headCx - 2f, waistY, headCx + 10f, waistY + 8f, legPaint)
                    canvas.drawLine(headCx + 10f, waistY + 8f, headCx + 16f, waistY + 17f, legPaint)
                    canvas.drawLine(headCx + 10f, waistY + 8f, headCx + 16f, waistY + 17f, legStripePaint)
                    canvas.drawCircle(headCx + 16f, waistY + 17f, 3.5f, shoePaint)

                    // Right leg kicks back
                    canvas.drawLine(headCx + 2f, waistY, headCx - 9f, waistY + 7f, legPaint)
                    canvas.drawLine(headCx - 9f, waistY + 7f, headCx - 17f, waistY + 15f, legPaint)
                    canvas.drawLine(headCx - 9f, waistY + 7f, headCx - 17f, waistY + 15f, legStripePaint)
                    canvas.drawCircle(headCx - 17f, waistY + 15f, 3.5f, shoePaint)

                    // Arms opposite
                    canvas.drawLine(headCx - 5f, chestY + 2f, headCx + 6f, chestY + 7f, armPaint)
                    canvas.drawLine(headCx + 6f, chestY + 7f, headCx + 14f, chestY + 4f, armPaint)

                    canvas.drawLine(headCx + 6f, chestY + 2f, headCx - 2f, chestY + 8f, armPaint)
                    canvas.drawLine(headCx - 2f, chestY + 8f, headCx - 9f, chestY + 5f, armPaint)
                }
            }
        } else {
            // Standing athletic idle stance
            canvas.drawLine(headCx - 4f, waistY, headCx - 6f, waistY + 18f, legPaint)
            canvas.drawCircle(headCx - 6f, waistY + 18f, 3.5f, shoePaint)

            canvas.drawLine(headCx + 4f, waistY, headCx + 6f, waistY + 18f, legPaint)
            canvas.drawCircle(headCx + 6f, waistY + 18f, 3.5f, shoePaint)

            canvas.drawLine(headCx - 6f, chestY + 2f, headCx - 9f, chestY + 13f, armPaint)
            canvas.drawLine(headCx + 6f, chestY + 2f, headCx + 9f, chestY + 13f, armPaint)
        }

        // 6. Modern Glassmorphism Runner Badge ("🏃 BẠN (LIVE)")
        val badgeW = 90f
        val badgeH = 26f
        val badgeY = 106f
        val pillRect = RectF(cx - badgeW / 2f, badgeY, cx + badgeW / 2f, badgeY + badgeH)

        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F00B132B")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(pillRect, 8f, 8f, badgeBgPaint)

        val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00F0FF")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(pillRect, 8f, 8f, badgeBorderPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00F0FF")
            textSize = 15f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val drawable = BitmapDrawable(context.resources, bitmap)
        runnerCache[key] = drawable
        return drawable
    }

    /**
     * Generates a terrifying high-definition animated frame for a Zombie character.
     *
     * @param zombieType 0 = Alpha Leader (Crimson), 1 = Stalker (Toxic Orange), 2 = Mutant (Neon Purple).
     */
    fun createZombieFrameDrawable(
        context: Context,
        frame: Int,
        distMeters: Int,
        alertLevel: ZombieAlertLevel,
        zombieType: Int
    ): BitmapDrawable {
        val key = "$zombieType-$frame-$distMeters"
        zombieCache.get(key)?.let { return it }

        val w = 130
        val h = 156
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cx = w / 2f
        val cy = 54f

        val (primaryColor, darkBgColor, nameTitle) = when (zombieType) {
            0 -> Triple(Color.parseColor("#EF4444"), Color.parseColor("#1C0606"), "ALPHA")
            1 -> Triple(Color.parseColor("#F97316"), Color.parseColor("#1C0C04"), "STALKER")
            else -> Triple(Color.parseColor("#A855F7"), Color.parseColor("#14061F"), "MUTANT")
        }

        // 1. Ominous Ground Shadow with Toxic Pool
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#66000000")
            style = Paint.Style.FILL
        }
        canvas.drawOval(cx - 24f, cy + 34f, cx + 24f, cy + 42f, shadowPaint)

        // 2. Violent Terror Shockwave Rings
        val waveRadius = 36f + (frame * 4f)
        val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            alpha = (140 - frame * 30).coerceIn(20, 140)
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }
        canvas.drawCircle(cx, cy, waveRadius, wavePaint)

        // 3. Volcanic Cracked Stone Base Disc
        val discRadius = 33f
        val discPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, discRadius,
                intArrayOf(darkBgColor, Color.parseColor("#090202")),
                floatArrayOf(0.4f, 1.0f),
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, discRadius, discPaint)

        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }
        canvas.drawCircle(cx, cy, discRadius, rimPaint)

        // Jagged Volcanic Cracks on Disc
        val crackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            strokeWidth = 1.8f
            style = Paint.Style.STROKE
            alpha = 180
        }
        canvas.drawLine(cx - 14f, cy - 10f, cx - 4f, cy + 2f, crackPaint)
        canvas.drawLine(cx - 4f, cy + 2f, cx - 18f, cy + 18f, crackPaint)
        canvas.drawLine(cx + 8f, cy - 16f, cx + 16f, cy - 4f, crackPaint)
        canvas.drawLine(cx + 16f, cy - 4f, cx + 10f, cy + 14f, crackPaint)

        // 4. Violent Blood Slash Streak (Frame 1 lunge effect)
        if (frame == 1) {
            val slashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryColor
                strokeWidth = 3.5f
                strokeCap = Paint.Cap.ROUND
                style = Paint.Style.STROKE
            }
            val slashPath = Path().apply {
                moveTo(cx + 12f, cy - 18f)
                quadTo(cx + 26f, cy - 2f, cx + 18f, cy + 16f)
            }
            canvas.drawPath(slashPath, slashPaint)
        }

        // 5. Terrifying Apocalyptic Zombie Character
        val lungeX = when (frame) {
            1 -> 6f  // violent forward thrust
            3 -> 3f  // aggressive dual-reach
            else -> -1f
        }
        val bobY = when (frame) {
            0 -> -3f
            1 -> 2f
            2 -> -1f
            3 -> 3f
            else -> 0f
        }

        val zx = cx + lungeX
        val zy = cy - 14f + bobY

        // Rotting Skull & Jaw
        val skullPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when (zombieType) {
                2 -> Color.parseColor("#7E22CE") // purple mutant
                else -> Color.parseColor("#4D7C0F") // toxic green rot
            }
            style = Paint.Style.FILL
        }
        canvas.drawCircle(zx, zy, 8.5f, skullPaint)

        // Sunken dark eye sockets
        val socketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#170202")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(zx + 3f, zy - 1.5f, 3.2f, socketPaint)
        canvas.drawCircle(zx - 4f, zy - 1.5f, 3.2f, socketPaint)

        // Flaring Crimson Slit Eyes with glowing trail
        val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF0055")
            style = Paint.Style.FILL
        }
        val flareR = if (frame == 1 || frame == 3) 2.5f else 1.8f
        canvas.drawCircle(zx + 3f, zy - 1.5f, flareR, eyePaint)
        canvas.drawCircle(zx - 4f, zy - 1.5f, flareR, eyePaint)

        // Eye flare streak on lunge
        if (frame == 1) {
            val flarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FF0055")
                strokeWidth = 2.5f
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawLine(zx + 3f, zy - 1.5f, zx + 12f, zy - 3.5f, flarePaint)
        }

        // Gaping bloody jaw with sharp fangs
        val mouthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#7F1D1D")
            style = Paint.Style.FILL
        }
        canvas.drawRect(zx - 4f, zy + 3.5f, zx + 4f, zy + 7.5f, mouthPaint)

        // Sharp white fangs
        val fangPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeWidth = 1.5f
        }
        canvas.drawLine(zx - 2.5f, zy + 3.5f, zx - 2.5f, zy + 6f, fangPaint)
        canvas.drawLine(zx + 2.5f, zy + 3.5f, zx + 2.5f, zy + 6f, fangPaint)

        // Hunched Torso in shredded apocalyptic rags
        val chestY = zy + 8f
        val waistY = chestY + 16f
        val torsoPath = Path().apply {
            moveTo(zx - 8f, chestY)
            lineTo(zx + 8f, chestY)
            lineTo(zx + 6f, waistY)
            lineTo(zx - 6f, waistY)
            close()
        }
        val ragsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#370A0A")
            style = Paint.Style.FILL
        }
        canvas.drawPath(torsoPath, ragsPaint)

        // Spined hunched back spikes
        val spikePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1917")
            strokeWidth = 2.5f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(zx - 7f, chestY + 3f, zx - 13f, chestY - 1f, spikePaint)
        canvas.drawLine(zx - 6f, chestY + 9f, zx - 12f, chestY + 6f, spikePaint)

        // Massive Skeletal Claws & Shambling Legs
        val armPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 6f
            strokeCap = Paint.Cap.ROUND
            color = when (zombieType) {
                2 -> Color.parseColor("#6B21A8")
                else -> Color.parseColor("#3F6212")
            }
            style = Paint.Style.STROKE
        }
        val clawTipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#171717")
            style = Paint.Style.FILL
        }
        val bloodTipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DC2626")
            style = Paint.Style.FILL
        }
        val legPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 5.5f
            strokeCap = Paint.Cap.ROUND
            color = Color.parseColor("#292524")
            style = Paint.Style.STROKE
        }

        when (frame) {
            0 -> {
                // Giant right claw cocked back, ready to strike
                canvas.drawLine(zx + 6f, chestY + 2f, zx + 14f, chestY + 4f, armPaint)
                canvas.drawLine(zx + 14f, chestY + 4f, zx + 20f, chestY + 11f, armPaint)
                canvas.drawCircle(zx + 20f, chestY + 11f, 3.5f, clawTipPaint)
                canvas.drawCircle(zx + 22f, chestY + 12f, 2f, bloodTipPaint)

                canvas.drawLine(zx - 6f, chestY + 2f, zx - 10f, chestY + 10f, armPaint)

                // Dragging shambling leg
                canvas.drawLine(zx + 3f, waistY, zx + 8f, waistY + 17f, legPaint)
                canvas.drawLine(zx - 3f, waistY, zx - 11f, waistY + 15f, legPaint)
            }
            1 -> {
                // DUAL CLAW VICIOUS SWIPE DOWNWARD!
                canvas.drawLine(zx + 6f, chestY + 2f, zx + 18f, chestY + 1f, armPaint)
                canvas.drawLine(zx + 18f, chestY + 1f, zx + 25f, chestY + 9f, armPaint)
                canvas.drawCircle(zx + 25f, chestY + 9f, 4f, clawTipPaint)
                canvas.drawCircle(zx + 27f, chestY + 11f, 2.5f, bloodTipPaint)

                canvas.drawLine(zx - 5f, chestY + 2f, zx + 12f, chestY + 6f, armPaint)
                canvas.drawLine(zx + 12f, chestY + 6f, zx + 20f, chestY + 15f, armPaint)
                canvas.drawCircle(zx + 20f, chestY + 15f, 3.5f, clawTipPaint)

                // Heavy stomping stride
                canvas.drawLine(zx + 4f, waistY, zx + 11f, waistY + 17f, legPaint)
                canvas.drawLine(zx - 2f, waistY, zx - 4f, waistY + 16f, legPaint)
            }
            2 -> {
                // Left claw slashing forward, Right claw pulling back
                canvas.drawLine(zx - 6f, chestY + 2f, zx + 14f, chestY + 4f, armPaint)
                canvas.drawLine(zx + 14f, chestY + 4f, zx + 20f, chestY + 11f, armPaint)
                canvas.drawCircle(zx + 20f, chestY + 11f, 3.5f, clawTipPaint)

                canvas.drawLine(zx + 6f, chestY + 2f, zx + 9f, chestY + 11f, armPaint)

                canvas.drawLine(zx - 3f, waistY, zx + 8f, waistY + 17f, legPaint)
                canvas.drawLine(zx + 3f, waistY, zx - 10f, waistY + 15f, legPaint)
            }
            3 -> {
                // FEROCIOUS DUAL CLAW GRASP REACHING FORWARD!
                canvas.drawLine(zx + 6f, chestY + 2f, zx + 19f, chestY + 3f, armPaint)
                canvas.drawLine(zx + 19f, chestY + 3f, zx + 26f, chestY + 12f, armPaint)
                canvas.drawCircle(zx + 26f, chestY + 12f, 4f, clawTipPaint)
                canvas.drawCircle(zx + 28f, chestY + 14f, 2.5f, bloodTipPaint)

                canvas.drawLine(zx - 6f, chestY + 2f, zx + 14f, chestY + 8f, armPaint)
                canvas.drawLine(zx + 14f, chestY + 8f, zx + 22f, chestY + 16f, armPaint)
                canvas.drawCircle(zx + 22f, chestY + 16f, 3.5f, clawTipPaint)

                canvas.drawLine(zx + 3f, waistY, zx + 9f, waistY + 17f, legPaint)
                canvas.drawLine(zx - 3f, waistY, zx - 6f, waistY + 16f, legPaint)
            }
        }

        // 6. Modern Glassmorphism Terror Badge ("👑 ALPHA 65m")
        val badgeW = 96f
        val badgeH = 26f
        val badgeY = 106f
        val pillRect = RectF(cx - badgeW / 2f, badgeY, cx + badgeW / 2f, badgeY + badgeH)

        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F0180404")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(pillRect, 8f, 8f, badgeBgPaint)

        val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(pillRect, 8f, 8f, badgeBorderPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            textSize = 14f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val prefix = when (zombieType) {
            0 -> "👑"
            1 -> "⚡"
            else -> "☣️"
        }
        canvas.drawText("$prefix $nameTitle ${distMeters}m", cx, badgeY + 18f, textPaint)

        val drawable = BitmapDrawable(context.resources, bitmap)
        zombieCache.put(key, drawable)
        return drawable
    }
}
