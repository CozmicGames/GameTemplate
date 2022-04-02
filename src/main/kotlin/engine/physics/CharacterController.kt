package engine.physics

import com.gratedgames.Kore
import com.gratedgames.utils.events.EventContext
import com.gratedgames.utils.maths.Vector2
import com.gratedgames.utils.maths.smoothDamp
import engine.Game

class CharacterController(var body: Body) {
    class LandedEvent(val body: Body)

    var jumpForce = 100.0f
    var crouchSpeed = 0.5f
    var movementSmoothing = 0.05f
    var doAllowAirControl = false
    var maxExtraJumpCount = 0

    var isOnGround = false
        private set

    private val velocity = Vector2()
    private var jumpCount = 0

    fun update() {
        val wasOnGround = isOnGround
        isOnGround = false

        val groundCheckX = body.bounds.centerX
        val groundCheckY = body.bounds.minY
        val groundCheckRadius = 0.2f

        Game.physics.forEachOverlappingCircle(groundCheckX, groundCheckY, groundCheckRadius) {
            if (it != body)
                isOnGround = true
        }

        if (isOnGround)
            jumpCount = 0

        if (isOnGround && !wasOnGround)
            Kore.context.inject<EventContext>()?.dispatch(LandedEvent(body))
    }

    fun move(amount: Float, crouch: Boolean, jump: Boolean, delta: Float) {
        var move = amount

        if (isOnGround || doAllowAirControl) {
            if (crouch) {
                move *= crouchSpeed
            }

            val targetVelocity = Vector2(move, body.velocity.y)
            body.velocity.set(smoothDamp(body.velocity, targetVelocity, velocity, movementSmoothing, Float.MAX_VALUE, delta))
        }

        if ((isOnGround || jumpCount - 1 < maxExtraJumpCount) && jump) {
            isOnGround = false
            body.applyForce(0.0f, jumpForce * -Game.physics.gravity.y)
            //body.velocity.y += jumpForce
            jumpCount++
        }
    }
}