package com.fittrack.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import com.fittrack.app.data.model.ZombieAlertLevel

/**
 * Generates frame-by-frame animated vector sprites for both Runner and Zombie Horde.
 * Provides lively running stride cycles, lunging claw attacks, pulsing aura rings,
 * and high-visibility status badges.
 */
object MarkerAnimationFactory {

    /**
     * Creates an animated frame for the Runner character.
     *
     * @param frame Current animation step [0..3].
     * @param isMoving Whether the runner is actively moving (sprinting) vs idle.
     */
    fun createRunnerFrameDrawable(
        context: Context,
        frame: Int,
        isMoving: Boolean
    ): BitmapDrawable {
        val w = 110
        val h = 130
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cx = w / 2f
        val cy = 48f

        // 1. Pulsing Cyan Energy Aura
        val pulseRadius = if (isMoving) 38f + (frame * 2.5f) else 38f
        val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4006B6D4")
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }
        canvas.drawCircle(cx, cy, pulseRadius, pulsePaint)

        // Dark circular base badge
        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, 34f, basePaint)

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#06B6D4")
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }
        canvas.drawCircle(cx, cy, 34f, ringPaint)

        // 2. Athletic Motion Blur / Speed Lines (when moving)
        if (isMoving) {
            val speedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#8022D3EE")
                strokeWidth = 2.5f
                strokeCap = Paint.Cap.ROUND
            }
            val offset = (frame % 2) * 3f
            canvas.drawLine(cx - 22f, cy - 6f + offset, cx - 32f, cy - 6f + offset, speedPaint)
            canvas.drawLine(cx - 24f, cy + 4f - offset, cx - 36f, cy + 4f - offset, speedPaint)
            canvas.drawLine(cx - 20f, cy + 14f + offset, cx - 30f, cy + 14f + offset, speedPaint)
        }

        // 3. Animated Runner Humanoid Figure
        val bobY = if (isMoving) {
            when (frame) {
                1, 3 -> -3f // airborne bounce mid-stride
                else -> 1f  // foot impact
            }
        } else 0f

        val skinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FDBA74") // healthy warm skin
            style = Paint.Style.FILL
        }
        val hairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            style = Paint.Style.FILL
        }
        val headbandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#22D3EE")
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }
        val shirtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#06B6D4")
            style = Paint.Style.FILL
        }
        val limbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 4f
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }
        val shoePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FACC15") // bright yellow running shoes
            style = Paint.Style.FILL
        }

        // Head
        val headY = cy - 14f + bobY
        canvas.drawCircle(cx, headY, 7.5f, skinPaint)
        // Hair & Headband
        canvas.drawArc(RectF(cx - 8f, headY - 8f, cx + 8f, headY + 3f), 180f, 180f, true, hairPaint)
        canvas.drawLine(cx - 7f, headY - 2f, cx + 7f, headY - 2f, headbandPaint)

        // Torso (leaning forward)
        val chestTopY = headY + 8f
        val hipY = chestTopY + 14f
        val torsoPath = Path().apply {
            moveTo(cx - 6f, chestTopY)
            lineTo(cx + 6f, chestTopY)
            lineTo(cx + 4f, hipY)
            lineTo(cx - 4f, hipY)
            close()
        }
        canvas.drawPath(torsoPath, shirtPaint)

        // Animated Limbs (Legs & Arms Stride Cycle)
        if (isMoving) {
            when (frame) {
                0 -> {
                    // Right leg forward, left leg back
                    limbPaint.color = Color.parseColor("#38BDF8")
                    // Right leg (front)
                    canvas.drawLine(cx + 2f, hipY, cx + 8f, hipY + 8f, limbPaint)
                    canvas.drawLine(cx + 8f, hipY + 8f, cx + 14f, hipY + 16f, limbPaint)
                    canvas.drawCircle(cx + 14f, hipY + 16f, 3f, shoePaint)

                    // Left leg (back)
                    limbPaint.color = Color.parseColor("#0284C7")
                    canvas.drawLine(cx - 2f, hipY, cx - 8f, hipY + 7f, limbPaint)
                    canvas.drawLine(cx - 8f, hipY + 7f, cx - 14f, hipY + 14f, limbPaint)
                    canvas.drawCircle(cx - 14f, hipY + 14f, 3f, shoePaint)

                    // Arms: Left arm forward, Right arm back
                    limbPaint.color = Color.parseColor("#FDBA74")
                    canvas.drawLine(cx + 4f, chestTopY + 2f, cx - 2f, chestTopY + 8f, limbPaint)
                    canvas.drawLine(cx - 2f, chestTopY + 8f, cx - 8f, chestTopY + 6f, limbPaint)

                    canvas.drawLine(cx - 4f, chestTopY + 2f, cx + 5f, chestTopY + 7f, limbPaint)
                    canvas.drawLine(cx + 5f, chestTopY + 7f, cx + 12f, chestTopY + 5f, limbPaint)
                }
                1, 3 -> {
                    // Mid-stride passing phase (knees bent, compact)
                    limbPaint.color = Color.parseColor("#38BDF8")
                    canvas.drawLine(cx + 2f, hipY, cx + 5f, hipY + 7f, limbPaint)
                    canvas.drawLine(cx + 5f, hipY + 7f, cx + 6f, hipY + 15f, limbPaint)
                    canvas.drawCircle(cx + 6f, hipY + 15f, 3f, shoePaint)

                    limbPaint.color = Color.parseColor("#0284C7")
                    canvas.drawLine(cx - 2f, hipY, cx - 5f, hipY + 7f, limbPaint)
                    canvas.drawLine(cx - 5f, hipY + 7f, cx - 6f, hipY + 15f, limbPaint)
                    canvas.drawCircle(cx - 6f, hipY + 15f, 3f, shoePaint)

                    limbPaint.color = Color.parseColor("#FDBA74")
                    canvas.drawLine(cx + 4f, chestTopY + 2f, cx + 6f, chestTopY + 9f, limbPaint)
                    canvas.drawLine(cx - 4f, chestTopY + 2f, cx - 6f, chestTopY + 9f, limbPaint)
                }
                2 -> {
                    // Left leg forward, right leg back
                    limbPaint.color = Color.parseColor("#38BDF8")
                    // Left leg (front)
                    canvas.drawLine(cx - 2f, hipY, cx + 8f, hipY + 8f, limbPaint)
                    canvas.drawLine(cx + 8f, hipY + 8f, cx + 14f, hipY + 16f, limbPaint)
                    canvas.drawCircle(cx + 14f, hipY + 16f, 3f, shoePaint)

                    // Right leg (back)
                    limbPaint.color = Color.parseColor("#0284C7")
                    canvas.drawLine(cx + 2f, hipY, cx - 8f, hipY + 7f, limbPaint)
                    canvas.drawLine(cx - 8f, hipY + 7f, cx - 14f, hipY + 14f, limbPaint)
                    canvas.drawCircle(cx - 14f, hipY + 14f, 3f, shoePaint)

                    // Arms opposite
                    limbPaint.color = Color.parseColor("#FDBA74")
                    canvas.drawLine(cx - 4f, chestTopY + 2f, cx + 5f, chestTopY + 7f, limbPaint)
                    canvas.drawLine(cx + 5f, chestTopY + 7f, cx + 12f, chestTopY + 5f, limbPaint)

                    canvas.drawLine(cx + 4f, chestTopY + 2f, cx - 2f, chestTopY + 8f, limbPaint)
                    canvas.drawLine(cx - 2f, chestTopY + 8f, cx - 8f, chestTopY + 6f, limbPaint)
                }
            }
        } else {
            // Idle stance (relaxed)
            limbPaint.color = Color.parseColor("#38BDF8")
            canvas.drawLine(cx - 3f, hipY, cx - 5f, hipY + 16f, limbPaint)
            canvas.drawCircle(cx - 5f, hipY + 16f, 3f, shoePaint)

            canvas.drawLine(cx + 3f, hipY, cx + 5f, hipY + 16f, limbPaint)
            canvas.drawCircle(cx + 5f, hipY + 16f, 3f, shoePaint)

            limbPaint.color = Color.parseColor("#FDBA74")
            canvas.drawLine(cx - 5f, chestTopY + 2f, cx - 8f, chestTopY + 12f, limbPaint)
            canvas.drawLine(cx + 5f, chestTopY + 2f, cx + 8f, chestTopY + 12f, limbPaint)
        }

        // 4. Runner High-Contrast Badge ("🏃 BẠN")
        val badgeW = 76f
        val badgeH = 24f
        val badgeY = 94f
        val pillRect = RectF(cx - badgeW / 2f, badgeY, cx + badgeW / 2f, badgeY + badgeH)

        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E60F172A")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(pillRect, 8f, 8f, badgeBgPaint)

        val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#06B6D4")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(pillRect, 8f, 8f, badgeBorderPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#22D3EE")
            textSize = 15f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("🏃 BẠN", cx, badgeY + 17f, textPaint)

        return BitmapDrawable(context.resources, bitmap)
    }

    /**
     * Creates an animated frame for a Zombie character (Leader Alpha, Flanker 1, or Flanker 2).
     *
     * @param frame Current animation step [0..3].
     * @param distMeters Rounded distance to runner.
     * @param alertLevel Severity level (Safe, Warning, Danger, Caught).
     * @param zombieType 0 = Alpha Leader (Red), 1 = Stalker Flank (Orange), 2 = Mutant (Purple/Crimson).
     */
    fun createZombieFrameDrawable(
        context: Context,
        frame: Int,
        distMeters: Int,
        alertLevel: ZombieAlertLevel,
        zombieType: Int
    ): BitmapDrawable {
        val w = 110
        val h = 130
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cx = w / 2f
        val cy = 48f

        // Theme colors based on zombie type and alert level
        val primaryColor = when (zombieType) {
            0 -> if (alertLevel == ZombieAlertLevel.DANGER || alertLevel == ZombieAlertLevel.CAUGHT) Color.parseColor("#EF4444") else Color.parseColor("#DC2626")
            1 -> Color.parseColor("#F97316") // Orange stalker
            else -> Color.parseColor("#A855F7") // Purple toxic mutant
        }

        // 1. Ominous Expanding Fear Ring
        val auraRadius = 34f + (frame * 3.5f)
        val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            alpha = (90 - frame * 18).coerceIn(20, 100)
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }
        canvas.drawCircle(cx, cy, auraRadius, auraPaint)

        // Dark bloody platform
        val platformPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C0A0A")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, 32f, platformPaint)

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }
        canvas.drawCircle(cx, cy, 32f, ringPaint)

        // 2. Animated Decayed Zombie Character
        // Shambling lunge & bob
        val lungeX = when (frame) {
            1, 3 -> 4f // forward lunge towards runner
            else -> -1f
        }
        val bobY = when (frame) {
            0 -> -2f
            1 -> 2f
            2 -> -1f
            3 -> 3f
            else -> 0f
        }

        val skinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when (zombieType) {
                2 -> Color.parseColor("#7E22CE") // purple mutant skin
                else -> Color.parseColor("#65A30D") // toxic rotting green skin
            }
            style = Paint.Style.FILL
        }
        val raggedClothesPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#450A0A") // blood-stained rags
            style = Paint.Style.FILL
        }
        val clawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 4f
            strokeCap = Paint.Cap.ROUND
            color = Color.parseColor("#4D7C0F")
            style = Paint.Style.STROKE
        }
        val clawTipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#171717") // sharp black claws
            style = Paint.Style.FILL
        }
        val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF1744") // bright glowing red eyes
            style = Paint.Style.FILL
        }

        // Head (leaning forward)
        val headX = cx + lungeX
        val headY = cy - 12f + bobY
        canvas.drawCircle(headX, headY, 7.5f, skinPaint)

        // Rotten Hair Tufts
        val hairTuftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(headX - 4f, headY - 7f, headX - 6f, headY - 12f, hairTuftPaint)
        canvas.drawLine(headX + 2f, headY - 7f, headX + 4f, headY - 13f, hairTuftPaint)

        // Evil Glowing Red Eyes (flares brightly on lunges)
        val eyeRadius = if (frame == 1 || frame == 3) 2.6f else 1.8f
        canvas.drawCircle(headX + 2.5f, headY - 1.5f, eyeRadius, eyePaint)
        canvas.drawCircle(headX - 3.5f, headY - 1.5f, eyeRadius, eyePaint)

        // Gaping Rotting Jaw / Teeth
        val jawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#991B1B") // bloody mouth interior
            style = Paint.Style.FILL
        }
        canvas.drawRect(headX - 3f, headY + 3f, headX + 3f, headY + 6f, jawPaint)

        // Torso in shredded rags
        val chestTopY = headY + 7f
        val hipY = chestTopY + 13f
        val torsoPath = Path().apply {
            moveTo(headX - 6f, chestTopY)
            lineTo(headX + 7f, chestTopY)
            lineTo(headX + 5f, hipY)
            lineTo(headX - 5f, hipY)
            close()
        }
        canvas.drawPath(torsoPath, raggedClothesPaint)

        // Animated Lunging Skeletal Claws & Shambling Legs
        when (frame) {
            0 -> {
                // Right arm reaching forward with sharp claws
                canvas.drawLine(headX + 5f, chestTopY + 3f, headX + 13f, chestTopY + 5f, clawPaint)
                canvas.drawLine(headX + 13f, chestTopY + 5f, headX + 18f, chestTopY + 10f, clawPaint)
                canvas.drawCircle(headX + 18f, chestTopY + 10f, 2.5f, clawTipPaint)

                // Left arm hunched
                canvas.drawLine(headX - 5f, chestTopY + 3f, headX - 8f, chestTopY + 9f, clawPaint)

                // Dragging leg
                clawPaint.color = Color.parseColor("#3F6212")
                canvas.drawLine(headX + 2f, hipY, headX + 6f, hipY + 14f, clawPaint)
                canvas.drawLine(headX - 3f, hipY, headX - 9f, hipY + 13f, clawPaint)
            }
            1 -> {
                // DOUBLE CLAW SWIPE FORWARD (Fierce attack)
                canvas.drawLine(headX + 5f, chestTopY + 3f, headX + 16f, chestTopY + 2f, clawPaint)
                canvas.drawLine(headX + 16f, chestTopY + 2f, headX + 22f, chestTopY + 8f, clawPaint)
                canvas.drawCircle(headX + 22f, chestTopY + 8f, 3f, clawTipPaint)

                canvas.drawLine(headX - 4f, chestTopY + 3f, headX + 10f, chestTopY + 6f, clawPaint)
                canvas.drawLine(headX + 10f, chestTopY + 6f, headX + 18f, chestTopY + 12f, clawPaint)
                canvas.drawCircle(headX + 18f, chestTopY + 12f, 3f, clawTipPaint)

                // Stomping legs
                clawPaint.color = Color.parseColor("#3F6212")
                canvas.drawLine(headX + 3f, hipY, headX + 9f, hipY + 15f, clawPaint)
                canvas.drawLine(headX - 2f, hipY, headX - 4f, hipY + 14f, clawPaint)
            }
            2 -> {
                // Left arm reaching forward, Right arm back
                canvas.drawLine(headX - 5f, chestTopY + 3f, headX + 13f, chestTopY + 5f, clawPaint)
                canvas.drawLine(headX + 13f, chestTopY + 5f, headX + 18f, chestTopY + 10f, clawPaint)
                canvas.drawCircle(headX + 18f, chestTopY + 10f, 2.5f, clawTipPaint)

                canvas.drawLine(headX + 5f, chestTopY + 3f, headX + 8f, chestTopY + 9f, clawPaint)

                clawPaint.color = Color.parseColor("#3F6212")
                canvas.drawLine(headX - 2f, hipY, headX + 7f, hipY + 14f, clawPaint)
                canvas.drawLine(headX + 3f, hipY, headX - 8f, hipY + 13f, clawPaint)
            }
            3 -> {
                // JAW SNAP & DUAL CLAW GRASP
                canvas.drawLine(headX + 5f, chestTopY + 3f, headX + 18f, chestTopY + 4f, clawPaint)
                canvas.drawLine(headX + 18f, chestTopY + 4f, headX + 23f, chestTopY + 11f, clawPaint)
                canvas.drawCircle(headX + 23f, chestTopY + 11f, 3f, clawTipPaint)

                canvas.drawLine(headX - 5f, chestTopY + 3f, headX + 12f, chestTopY + 8f, clawPaint)
                canvas.drawLine(headX + 12f, chestTopY + 8f, headX + 19f, chestTopY + 14f, clawPaint)
                canvas.drawCircle(headX + 19f, chestTopY + 14f, 3f, clawTipPaint)

                clawPaint.color = Color.parseColor("#3F6212")
                canvas.drawLine(headX + 3f, hipY, headX + 8f, hipY + 14f, clawPaint)
                canvas.drawLine(headX - 3f, hipY, headX - 6f, hipY + 15f, clawPaint)
            }
        }

        // 3. High-Contrast Distance Badge ("🧟 65m")
        val badgeW = 76f
        val badgeH = 24f
        val badgeY = 94f
        val pillRect = RectF(cx - badgeW / 2f, badgeY, cx + badgeW / 2f, badgeY + badgeH)

        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E6180505")
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
            textSize = 15f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val labelPrefix = when (zombieType) {
            0 -> "🧟"
            1 -> "💀"
            else -> "☣️"
        }
        canvas.drawText("$labelPrefix ${distMeters}m", cx, badgeY + 17f, textPaint)

        return BitmapDrawable(context.resources, bitmap)
    }
}
