package net.gripff.amplifiermod.blocks;

import net.gripff.amplifiermod.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

public class AmplifierBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty SIGNAL_IN = BooleanProperty.create("signal_in");
    public static final BooleanProperty MODE = BooleanProperty.create("mode");
    public static final IntegerProperty TORCH_POS = IntegerProperty.create("torch_pos", 0, 3);
    public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 15);

    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 2, 16));

    public AmplifierBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(SIGNAL_IN, false)
                .setValue(MODE, false)
                .setValue(TORCH_POS, 0)
                .setValue(POWER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SIGNAL_IN, MODE, TORCH_POS, POWER);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected boolean shouldRedstoneWireConnectTo(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        if (direction == null) {
            return true;
        }
        Direction facing = state.getValue(FACING);
        return direction == facing || direction == facing.getOpposite();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            recompute(level, pos, state);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide()) {
            recompute(level, pos, state);
        }
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
    }

    private static void recompute(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        Direction inputSide = facing.getOpposite();
        BlockPos inputPos = pos.relative(inputSide);

        int inputSignal = level.getSignal(inputPos, inputSide);
        if (inputSignal == 0) {
            inputSignal = level.getDirectSignal(inputPos, inputSide);
        }

        int multiplier = getMultiplier(state.getValue(TORCH_POS));
        int outputSignal = calculateOutput(inputSignal, multiplier, state.getValue(MODE));

        boolean signalIn = inputSignal > 0;
        if (state.getValue(POWER) != outputSignal || state.getValue(SIGNAL_IN) != signalIn) {
            BlockState newState = state.setValue(POWER, outputSignal).setValue(SIGNAL_IN, signalIn);
            level.setBlock(pos, newState, Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockState newState = player.isShiftKeyDown()
                ? state.cycle(MODE)
                : state.cycle(TORCH_POS);

        if (player.isShiftKeyDown()) {
            boolean newMode = !state.getValue(MODE);
            level.playSound(null, pos,
                    newMode ? SoundEvents.STONE_BUTTON_CLICK_ON : SoundEvents.STONE_BUTTON_CLICK_OFF,
                    SoundSource.BLOCKS, 0.5F, 1.0F);
        }

        level.setBlock(pos, newState, Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);

        recompute(level, pos, newState);

        return InteractionResult.CONSUME;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        if (side == state.getValue(FACING).getOpposite()) {
            return state.getValue(POWER);
        }
        return 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        if (side == state.getValue(FACING).getOpposite()) {
            return state.getValue(POWER);
        }
        return 0;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        boolean signalIn = state.getValue(SIGNAL_IN);
        boolean modeOn   = state.getValue(MODE);

        if (!signalIn && !modeOn) {
            return;
        }

        Direction facing = state.getValue(FACING);
        int torchPos = state.getValue(TORCH_POS);

        double[] torchZ = {0.2, 0.4, 0.6, 0.8};

        double localX1 = 0.5;
        double localZ1 = torchZ[torchPos];
        double localX2 = 0.8;
        double localZ2 = 0.8;
        double localY  = 0.5;

        double[] p1 = rotateLocal(localX1, localZ1, facing);
        double[] p2 = rotateLocal(localX2, localZ2, facing);

        double wy = pos.getY() + localY;

        if (signalIn) {
            level.addParticle(DustParticleOptions.REDSTONE,
                    pos.getX() + p1[0], wy, pos.getZ() + p1[1],
                    0.0, 0.0, 0.0);
        }

        if (modeOn) {
            level.addParticle(DustParticleOptions.REDSTONE,
                    pos.getX() + p2[0], wy, pos.getZ() + p2[1],
                    0.0, 0.0, 0.0);
        }
    }

    private static double[] rotateLocal(double x, double z, Direction facing) {
        double cx = 0.5, cz = 0.5;
        double dx = x - cx;
        double dz = z - cz;
        double nx, nz;
        switch (facing) {
            case SOUTH -> { nx =  dx; nz =  dz; }              // базовая (y=0)
            case NORTH -> { nx = -dx; nz = -dz; }              // 180°
            case EAST  -> { nx =  dz; nz = -dx; }              // 270° (в JSON east = 270)
            case WEST  -> { nx = -dz; nz =  dx; }              // 90°
            default    -> { nx =  dx; nz =  dz; }
        }
        return new double[]{ nx + cx, nz + cz };
    }

    private static int getMultiplier(int torchPos) {
        return switch (torchPos) {
            case 0 -> Config.MULTIPLIER_POS_0.get();
            case 1 -> Config.MULTIPLIER_POS_1.get();
            case 2 -> Config.MULTIPLIER_POS_2.get();
            case 3 -> Config.MULTIPLIER_POS_3.get();
            default -> 1;
        };
    }

    private static int calculateOutput(int inputSignal, int multiplier, boolean divideMode) {
        if (multiplier <= 0) {
            return 0;
        }
        int result = divideMode ? inputSignal / multiplier : inputSignal * multiplier;
        return Mth.clamp(result, 0, 15);
    }
}