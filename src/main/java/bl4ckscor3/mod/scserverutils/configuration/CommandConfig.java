package bl4ckscor3.mod.scserverutils.configuration;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public record CommandConfig(BooleanValue enabled, IntValue permissionLevel, Supplier<BiConsumer<CommandDispatcher<CommandSourceStack>, Integer>> registrar) {}