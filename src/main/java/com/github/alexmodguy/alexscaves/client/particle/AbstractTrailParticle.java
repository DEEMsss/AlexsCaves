package com.github.alexmodguy.alexscaves.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import static net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

public abstract class AbstractTrailParticle extends Particle {

    private Vec3[] trailPositions = new Vec3[64];
    private int trailPointer = -1;

    protected float trailR = 1.0F;
    protected float trailG = 1.0F;
    protected float trailB = 1.0F;

    protected float trailA = 1.0F;

    public AbstractTrailParticle(ClientLevel world, double x, double y, double z, double xd, double yd, double zd) {
        super(world, x, y, z);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
    }

    public void tick() {
        tickTrail();
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.xd *= 0.99;
        this.yd *= 0.99;
        this.zd *= 0.99;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.move(this.xd, this.yd, this.zd);
            this.yd -= (double)this.gravity;
        }
    }

    public void tickTrail(){

        Vec3 currentPosition = new Vec3(this.x, this.y, this.z);
        if(trailPointer == -1){
            for(int i = 0; i < trailPositions.length; i++){
                trailPositions[i] = currentPosition;
            }
        }
        if (++this.trailPointer == this.trailPositions.length) {
            this.trailPointer = 0;
        }
        this.trailPositions[this.trailPointer] = currentPosition;
    }

    public void render(VertexConsumer consumer, Camera camera, float partialTick) {
        MultiBufferSource.BufferSource multibuffersource$buffersource = Minecraft.getInstance().renderBuffers().bufferSource();
        if(trailPointer > -1){
            Vec3 cameraPos = camera.getPosition();
            float x = (float)(Mth.lerp((double)partialTick, this.xo, this.x));
            float y = (float)(Mth.lerp((double)partialTick, this.yo, this.y));
            float z = (float)(Mth.lerp((double)partialTick, this.zo, this.z));

            PoseStack posestack = new PoseStack();
            posestack.pushPose();
            posestack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            int samples = 0;
            Vec3 drawFrom = new Vec3(x, y, z);
            VertexConsumer vertexconsumer = multibuffersource$buffersource.getBuffer(getTrailRenderType());
            Vec3 fromCamera = cameraPos.subtract(drawFrom);
            float zRot = getTrailRot(fromCamera);
            Vec3 topAngleVec = new Vec3(0, getTrailHeight() / 2F, 0).zRot(zRot);
            Vec3 bottomAngleVec = new Vec3(0, getTrailHeight() / -2F, 0).zRot(zRot);
            int j = getLightColor(partialTick);
            while(samples < sampleCount()){
                Vec3 sample = getTrailPosition(samples * sampleStep(), partialTick);
                float u1 = samples / (float)sampleCount();
                float u2 = u1 + 1 / (float)sampleCount();

                Vec3 draw1 = drawFrom;
                Vec3 draw2 = sample;

                PoseStack.Pose posestack$pose = posestack.last();
                Matrix4f matrix4f = posestack$pose.pose();
                Matrix3f matrix3f = posestack$pose.normal();
                vertexconsumer.vertex(matrix4f, (float) draw1.x + (float)bottomAngleVec.x, (float) draw1.y + (float)bottomAngleVec.y, (float) draw1.z + (float)bottomAngleVec.z).color(trailR, trailG, trailB, trailA).uv(u1, 1F).overlayCoords(NO_OVERLAY).uv2(j).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
                vertexconsumer.vertex(matrix4f, (float) draw2.x + (float)bottomAngleVec.x, (float) draw2.y + (float)bottomAngleVec.y, (float) draw2.z + (float)bottomAngleVec.z).color(trailR, trailG, trailB, trailA).uv(u2, 1F).overlayCoords(NO_OVERLAY).uv2(j).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
                vertexconsumer.vertex(matrix4f, (float) draw2.x + (float)topAngleVec.x, (float) draw2.y + (float)topAngleVec.y, (float) draw2.z + (float)topAngleVec.z).color(trailR, trailG, trailB, trailA).uv(u2, 0).overlayCoords(NO_OVERLAY).uv2(j).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
                vertexconsumer.vertex(matrix4f, (float) draw1.x + (float)topAngleVec.x, (float) draw1.y + (float)topAngleVec.y, (float) draw1.z + (float)topAngleVec.z).color(trailR, trailG, trailB, trailA).uv(u1, 0).overlayCoords(NO_OVERLAY).uv2(j).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
                samples++;
                drawFrom = sample;
            }
            posestack.popPose();
            multibuffersource$buffersource.endLastBatch();
        }
    }

    public float getTrailRot(Vec3 fromCamera) {
       return (float)(-Math.atan2((double)fromCamera.y, (double)fromCamera.horizontalDistance()));
    }

    public abstract float getTrailHeight();

    public abstract RenderType getTrailRenderType();

    public int sampleCount(){
        return 20;
    }

    public int sampleStep(){
        return 1;
    }

    protected float getU0(){
        return 0;
    }

    protected float getU1(){
        return 1;
    }

    protected float getV0(){
        return 0;
    }

    protected float getV1(){
        return 1;
    }

    public Vec3 getTrailPosition(int pointer, float partialTick) {
        if (this.removed) {
            partialTick = 1.0F;
        }
        int i = this.trailPointer - pointer & 63;
        int j = this.trailPointer - pointer - 1 & 63;
        Vec3 d0 = this.trailPositions[j];
        Vec3 d1 = this.trailPositions[i].subtract(d0);
        return d0.add(d1.scale(partialTick));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }
}