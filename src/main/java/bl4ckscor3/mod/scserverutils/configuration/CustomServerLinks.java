package bl4ckscor3.mod.scserverutils.configuration;

import java.util.List;

import com.mojang.datafixers.util.Either;

import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerLinks;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

public record CustomServerLinks(BooleanValue enabled, ConfigValue<List<? extends String>> serverLinks) {
	public List<ServerLinks.UntrustedEntry> resolve() {
		//@formatter:off
		return serverLinks.get()
				.stream()
				.map(s -> s.split("\\|"))
				.map(split -> new ServerLinks.UntrustedEntry(Either.right(Component.translatableWithFallback(split[1], split[2])), split[0]))
				.toList();
		//@formatter:on
	}
}