package io.redspace.ironsspellbooks.entity.spells;

import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.particle.ZapParticleOption;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.spells.SchoolType;
import io.redspace.ironsspellbooks.spells.SpellType;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import io.redspace.ironsspellbooks.util.Utils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class ChainLightning extends AbstractMagicProjectile {
    List<Entity> allVictims;
    List<Entity> lastVictims;
    Entity initialVictim;
    public int maxConnections = 4;
    public int maxConnectionsPerWave = 3;
    public float range = 3f;

    public ChainLightning(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        allVictims = new ArrayList<>();
        lastVictims = new ArrayList<>();
    }

    public ChainLightning(Level level, Entity owner, Entity initialVictim) {
        this(EntityRegistry.CHAIN_LIGHTNING.get(), level);
        this.setOwner(owner);
        this.setPos(initialVictim.position());
        this.initialVictim = initialVictim;
    }

    int hits;

    @Override
    public void tick() {
        super.tick();
        int f = tickCount - 1;
        if (!this.level.isClientSide && f % 6 == 0) {
            if (f == 0 && !allVictims.contains(initialVictim)) {
                //First time zap
                doHurt(initialVictim);
                if (getOwner() != null) {
                    Vec3 start = getOwner().position().add(0, getOwner().getBbHeight() / 2, 0);
                    PositionSource dest = new EntityPositionSource(initialVictim, initialVictim.getBbHeight() / 2);
                    ((ServerLevel) level).sendParticles(new ZapParticleOption(dest), start.x, start.y, start.z, 1, 0, 0, 0, 0);
                }

            } else {
                int j = lastVictims.size();
                AtomicInteger zapsThisWave = new AtomicInteger();
                //cannot be enhanced for
                for (int i = 0; i < j; i++) {
                    var entity = lastVictims.get(i);
                    var entities = level.getEntities(entity, entity.getBoundingBox().inflate(range), this::canHitEntity);
                    entities.sort((o1, o2) -> (int) (o1.distanceToSqr(entity) - o2.distanceToSqr(entity)));
                    entities.forEach((victim) -> {
                        if (zapsThisWave.get() < maxConnectionsPerWave && hits < maxConnections && victim.distanceToSqr(entity) < range * range && Utils.hasLineOfSight(level, entity.getEyePosition(), victim.getEyePosition(), true)) {
                            doHurt(victim);
                            victim.playSound(SoundRegistry.CHAIN_LIGHTNING_CHAIN.get(), 2, 1);
                            zapsThisWave.getAndIncrement();
                            Vec3 start = new Vec3(entity.xOld, entity.yOld, entity.zOld).add(0, entity.getBbHeight() / 2, 0);
                            PositionSource dest = new EntityPositionSource(victim, victim.getBbHeight() / 2);
                            ((ServerLevel) level).sendParticles(new ZapParticleOption(dest), start.x, start.y, start.z, 1, 0, 0, 0, 0);
                        }
                    });
                }
                lastVictims.removeAll(allVictims);
            }
            allVictims.addAll(lastVictims);
        }
    }

    public void doHurt(Entity victim) {
        hits++;
        DamageSources.applyDamage(victim, damage, SpellType.CHAIN_LIGHTNING_SPELL.getDamageSource(this, getOwner()), SchoolType.LIGHTNING);
        MagicManager.spawnParticles(level, ParticleHelper.ELECTRICITY, victim.getX(), victim.getY() + victim.getBbHeight() / 2, victim.getZ(), 10, victim.getBbWidth() / 3, victim.getBbHeight() / 3, victim.getBbWidth() / 3, 0.1, false);

        lastVictims.add(victim);
    }

    public boolean hasAlreadyZapped(Entity entity) {
        return allVictims.contains(entity) || lastVictims.contains(entity);
    }

    @Override
    protected boolean canHitEntity(Entity pTarget) {
        return !DamageSources.isFriendlyFireBetween(pTarget, getOwner()) && pTarget != getOwner() && !hasAlreadyZapped(pTarget) && super.canHitEntity(pTarget);
    }

    @Override
    public void trailParticles() {

    }

    @Override
    public void impactParticles(double x, double y, double z) {

    }

    @Override
    public float getSpeed() {
        return 0;
    }

    @Override
    public Optional<SoundEvent> getImpactSound() {
        return Optional.empty();
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
