package me.pepperbell.continuity.client.properties;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Iterators;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.pepperbell.continuity.api.client.CtmProperties;
import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.resource.ResourceRedirectHandler;
import me.pepperbell.continuity.client.util.MathUtil;
import me.pepperbell.continuity.client.util.biome.BiomeHolder;
import me.pepperbell.continuity.client.util.biome.BiomeHolderManager;
import me.pepperbell.continuity.client.util.biome.BiomeSetPredicate;
import net.minecraft.IdentifierException;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BaseCtmProperties implements CtmProperties {
	public static final Identifier SPECIAL_SKIP_ID = ContinuityClient.asId("special/skip");
	public static final Identifier SPECIAL_DEFAULT_ID = ContinuityClient.asId("special/default");

	protected static final int DIRECTION_AMOUNT = Direction.values().length;

	protected Properties properties;
	protected Identifier resourceId;
	protected String packId;
	protected int packPriority;
	protected ResourceManager resourceManager;
	protected String method;

	@Nullable
	protected Set<Identifier> matchTilesSet;
	@Nullable
	protected Predicate<BlockState> matchBlocksPredicate;
	protected List<Identifier> spriteIds = Collections.emptyList();
	protected Set<Identifier> spriteDependencies = Collections.emptySet();
	@Nullable
	protected EnumSet<Direction> faces;
	@Nullable
	protected Predicate<Biome> biomePredicate;
	@Nullable
	protected IntPredicate heightPredicate;
	@Nullable
	protected Predicate<String> blockEntityNamePredicate;

	protected boolean prioritized = false;

	protected boolean valid = true;

	public BaseCtmProperties(Properties properties, Identifier resourceId, PackResources pack, int packPriority, ResourceManager resourceManager, String method) {
		this.properties = properties;
		this.resourceId = resourceId;
		this.packId = pack.packId();
		this.packPriority = packPriority;
		this.resourceManager = resourceManager;
		this.method = method;
	}

	@Override
	public Set<Identifier> getSpriteDependencies() {
		return spriteDependencies;
	}

	// TODO: sorting API using Comparator
	/*
	-1 this < o
	0 this == o
	1 this > o
	 */
	@Override
	public int compareTo(@NotNull CtmProperties o) {
		if (o instanceof BaseCtmProperties o1) {
			if (prioritized && !o1.prioritized) {
				return 1;
			}
			if (!prioritized && o1.prioritized) {
				return -1;
			}
			int c = MathUtil.signum(packPriority - o1.packPriority);
			if (c != 0) {
				return c;
			}
			return o1.getResourceId().compareTo(getResourceId());
		}
		return 0;
	}

	public void init() {
		parseMatchTiles();
		parseMatchBlocks();
		detectMatches();
		validateMatches();
		parseTiles();
		parseFaces();
		parseBiomes();
		parseHeights();
		parseLegacyHeights();
		parseName();
		parsePrioritize();
		parseResourceCondition();
	}

	protected void parseMatchTiles() {
		matchTilesSet = PropertiesParsingHelper.parseMatchTiles(properties, "matchTiles", resourceId, packId);
		if (matchTilesSet != null && matchTilesSet.isEmpty()) {
			valid = false;
		}
	}

	protected void parseMatchBlocks() {
		matchBlocksPredicate = PropertiesParsingHelper.parseBlockStates(properties, "matchBlocks", resourceId, packId);
		if (matchBlocksPredicate == PropertiesParsingHelper.EMPTY_BLOCK_STATE_PREDICATE) {
			valid = false;
		}
	}

	protected void detectMatches() {
		String baseName = FilenameUtils.getBaseName(resourceId.getPath());
		if (matchBlocksPredicate == null) {
			if (baseName.startsWith("block_")) {
				try {
					Identifier id = Identifier.parse(baseName.substring(6));
					if (BuiltInRegistries.BLOCK.containsKey(id)) {
						Block block = BuiltInRegistries.BLOCK.getValue(id);
						matchBlocksPredicate = state -> state.getBlock() == block;
					}
				} catch (IdentifierException e) {
					//
				}
			}
		}
	}

	protected void validateMatches() {
		if (matchTilesSet == null && matchBlocksPredicate == null) {
			ContinuityClient.LOGGER.error("No tile or block matches provided in file '" + resourceId + "' in pack '" + packId + "'");
			valid = false;
		}
	}

	protected void parseTiles() {
		String tilesStr = properties.getProperty("tiles");
		if (tilesStr == null) {
			ContinuityClient.LOGGER.error("No 'tiles' value provided in file '" + resourceId + "' in pack '" + packId + "'");
			valid = false;
			return;
		}

		String[] tileStrs = tilesStr.trim().split("[ ,]");
		if (tileStrs.length != 0) {
			spriteIds = new ObjectArrayList<>();
			spriteDependencies = new ObjectOpenHashSet<>();

			String basePath = FilenameUtils.getPath(resourceId.getPath());
			String spriteBasePath;
			if (basePath.startsWith("textures/")) {
				spriteBasePath = basePath.substring(9);
			} else if (basePath.startsWith("optifine/")) {
				spriteBasePath = ResourceRedirectHandler.SPRITE_PATH_START + basePath.substring(9);
			} else {
				spriteBasePath = null;
			}

			for (int i = 0; i < tileStrs.length; i++) {
				String tileStr = tileStrs[i];
				if (tileStr.isEmpty()) {
					continue;
				}

				if (tileStr.endsWith("<skip>") || tileStr.endsWith("<skip>.png")) {
					spriteIds.add(SPECIAL_SKIP_ID);
					continue;
				} else if (tileStr.endsWith("<default>") || tileStr.endsWith("<default>.png")) {
					spriteIds.add(SPECIAL_DEFAULT_ID);
					continue;
				}

				String[] rangeParts = tileStr.split("-", 2);
				if (rangeParts.length != 0) {
					if (rangeParts.length == 2) {
						try {
							int min = Integer.parseInt(rangeParts[0]);
							int max = Integer.parseInt(rangeParts[1]);
							if (min <= max) {
								if (spriteBasePath != null) {
									try {
										for (int tile = min; tile <= max; tile++) {
											Identifier spriteId = resourceId.withPath(spriteBasePath + tile);
											spriteIds.add(spriteId);
											spriteDependencies.add(spriteId);
										}
									} catch (IdentifierException e) {
										ContinuityClient.LOGGER.warn("Invalid 'tiles' element '" + tileStr + "' at index " + i + " in file '" + resourceId + "' in pack '" + packId + "'", e);
									}
								} else {
									for (int tile = min; tile <= max; tile++) {
										spriteIds.add(MissingTextureAtlasSprite.getLocation());
									}
								}
							} else {
								ContinuityClient.LOGGER.warn("Invalid 'tiles' element '" + tileStr + "' at index " + i + " in file '" + resourceId + "' in pack '" + packId + "'");
							}
							continue;
						} catch (NumberFormatException e) {
							//
						}
					}

					String[] parts = tileStr.split(":", 2);
					if (parts.length != 0) {
						String namespace;
						String path;
						if (parts.length > 1) {
							namespace = parts[0];
							path = parts[1];
						} else {
							namespace = null;
							path = parts[0];
						}

						if (path.endsWith(".png")) {
							path = path.substring(0, path.length() - 4);
						}

						if (namespace == null) {
							if (path.startsWith("assets/minecraft/")) {
								path = path.substring(17);
							} else if (path.startsWith("./")) {
								path = basePath + path.substring(2);
							} else if (path.startsWith("~/")) {
								path = "optifine/" + path.substring(2);
							} else if (path.startsWith("/")) {
								path = "optifine/" + path.substring(1);
							}

							if (!path.startsWith("textures/") && !path.startsWith("optifine/")) {
								path = basePath + path;
							}

							if (path.startsWith("optifine/")) {
								namespace = resourceId.getNamespace();
							}
						} else {
							if (!path.contains("/")) {
								path = "textures/block/" + path;
							} else if (!path.startsWith("textures/") && !path.startsWith("optifine/")) {
								path = "textures/" + path;
							}
						}

						if (path.startsWith("textures/")) {
							path = path.substring(9);
						} else if (path.startsWith("optifine/")) {
							path = ResourceRedirectHandler.SPRITE_PATH_START + path.substring(9);
						} else {
							spriteIds.add(MissingTextureAtlasSprite.getLocation());
							continue;
						}

						if (namespace == null) {
							namespace = Identifier.DEFAULT_NAMESPACE;
						}

						try {
							Identifier spriteId = Identifier.fromNamespaceAndPath(namespace, path);
							spriteIds.add(spriteId);
							spriteDependencies.add(spriteId);
						} catch (IdentifierException e) {
							ContinuityClient.LOGGER.warn("Invalid 'tiles' element '" + tileStr + "' at index " + i + " in file '" + resourceId + "' in pack '" + packId + "'", e);
						}
					}
				} else {
					ContinuityClient.LOGGER.warn("Invalid 'tiles' element '" + tileStr + "' at index " + i + " in file '" + resourceId + "' in pack '" + packId + "'");
				}
			}
		}
	}

	protected void parseFaces() {
		String facesStr = properties.getProperty("faces");
		if (facesStr == null) {
			return;
		}

		String[] faceStrs = facesStr.trim().split("[ ,]");
		if (faceStrs.length != 0) {
			faces = EnumSet.noneOf(Direction.class);

			for (int i = 0; i < faceStrs.length; i++) {
				String faceStr = faceStrs[i];
				if (faceStr.isEmpty()) {
					continue;
				}

				String faceStr1 = faceStr.toUpperCase(Locale.ROOT);
				if (faceStr1.equals("BOTTOM")) {
					faces.add(Direction.DOWN);
				} else if (faceStr1.equals("TOP")) {
					faces.add(Direction.UP);
				} else if (faceStr1.equals("SIDES")) {
					Iterators.addAll(faces, Direction.Plane.HORIZONTAL.iterator());
				} else if (faceStr1.equals("ALL")) {
					faces = null;
					return;
				} else {
					try {
						faces.add(Direction.valueOf(faceStr1));
					} catch (IllegalArgumentException e) {
						ContinuityClient.LOGGER.warn("Unknown 'faces' element '" + faceStr + "' at index " + i + " in file '" + resourceId + "' in pack '" + packId + "'");
					}
				}
			}

			if (faces.isEmpty()) {
				valid = false;
			} else if (faces.size() == DIRECTION_AMOUNT) {
				faces = null;
			}
		} else {
			valid = false;
		}
	}

	protected void parseBiomes() {
		String biomesStr = properties.getProperty("biomes");
		if (biomesStr == null) {
			return;
		}

		biomesStr = biomesStr.trim();
		if (!biomesStr.isEmpty()) {
			boolean negate = false;
			if (biomesStr.charAt(0) == '!') {
				negate = true;
				biomesStr = biomesStr.substring(1);
			}

			String[] biomeStrs = biomesStr.split(" ");
			if (biomeStrs.length != 0) {
				ObjectOpenHashSet<BiomeHolder> biomeHolderSet = new ObjectOpenHashSet<>();

				for (int i = 0; i < biomeStrs.length; i++) {
					String biomeStr = biomeStrs[i];
					if (biomeStr.isEmpty()) {
						continue;
					}

					try {
						Identifier biomeId = Identifier.parse(biomeStr.toLowerCase(Locale.ROOT));
						biomeHolderSet.add(BiomeHolderManager.getOrCreateHolder(biomeId));
					} catch (IdentifierException e) {
						ContinuityClient.LOGGER.warn("Invalid 'biomes' element '" + biomeStr + "' at index " + i + " in file '" + resourceId + "' in pack '" + packId + "'", e);
					}
				}

				if (!biomeHolderSet.isEmpty()) {
					biomeHolderSet.trim();
					biomePredicate = new BiomeSetPredicate(biomeHolderSet);
					if (negate) {
						biomePredicate = biomePredicate.negate();
					}
				} else {
					if (!negate) {
						valid = false;
					}
				}
			} else {
				if (!negate) {
					valid = false;
				}
			}
		} else {
			valid = false;
		}
	}

	protected void parseHeights() {
		String heightsStr = properties.getProperty("heights");
		if (heightsStr == null) {
			return;
		}

		String[] heightStrs = heightsStr.trim().split("[ ,]");
		if (heightStrs.length != 0) {
			ObjectArrayList<IntPredicate> predicateList = new ObjectArrayList<>();

			for (int i = 0; i < heightStrs.length; i++) {
				String heightStr = heightStrs[i];
				if (heightStr.isEmpty()) {
					continue;
				}

				String[] parts = heightStr.split("\\.\\.", 2);
				if (parts.length == 2) {
					try {
						if (parts[1].isEmpty()) {
							int min = Integer.parseInt(parts[0]);
							predicateList.add(y -> y >= min);
						} else if (parts[0].isEmpty()) {
							int max = Integer.parseInt(parts[1]);
							predicateList.add(y -> y <= max);
						} else {
							int min = Integer.parseInt(parts[0]);
							int max = Integer.parseInt(parts[1]);
							if (min < max) {
								predicateList.add(y -> y >= min && y <= max);
							} else if (min > max) {
								predicateList.add(y -> y >= max && y <= min);
							} else {
								predicateList.add(y -> y == min);
							}
						}
						continue;
					} catch (NumberFormatException e) {
						//
					}
				} else if (parts.length == 1) {
					String heightStr1 = heightStr.replaceAll("[()]", "");
					if (!heightStr1.isEmpty()) {
						int separatorIndex = heightStr1.indexOf('-', heightStr1.charAt(0) == '-' ? 1 : 0);
						try {
							if (separatorIndex == -1) {
								int height = Integer.parseInt(heightStr1);
								predicateList.add(y -> y == height);
							} else {
								int min = Integer.parseInt(heightStr1.substring(0, separatorIndex));
								int max = Integer.parseInt(heightStr1.substring(separatorIndex + 1));
								if (min < max) {
									predicateList.add(y -> y >= min && y <= max);
								} else if (min > max) {
									predicateList.add(y -> y >= max && y <= min);
								} else {
									predicateList.add(y -> y == min);
								}
							}
							continue;
						} catch (NumberFormatException e) {
							//
						}
					}
				}
				ContinuityClient.LOGGER.warn("Invalid 'heights' element '" + heightStr + "' at index " + i + " in file '" + resourceId + "' in pack '" + packId + "'");
			}

			if (!predicateList.isEmpty()) {
				IntPredicate[] predicateArray = predicateList.toArray(IntPredicate[]::new);
				heightPredicate = y -> {
					for (IntPredicate predicate : predicateArray) {
						if (predicate.test(y)) {
							return true;
						}
					}
					return false;
				};
			} else {
				valid = false;
			}
		} else {
			valid = false;
		}
	}

	protected void parseLegacyHeights() {
		if (heightPredicate == null) {
			String minHeightStr = properties.getProperty("minHeight");
			String maxHeightStr = properties.getProperty("maxHeight");
			boolean hasMinHeight = minHeightStr != null;
			boolean hasMaxHeight = maxHeightStr != null;
			if (hasMinHeight || hasMaxHeight) {
				int min = 0;
				int max = 0;
				if (hasMinHeight) {
					try {
						min = Integer.parseInt(minHeightStr.trim());
					} catch (NumberFormatException e) {
						ContinuityClient.LOGGER.warn("Invalid 'minHeight' value '" + minHeightStr + "' in file '" + resourceId + "' in pack '" + packId + "'");
						hasMinHeight = false;
					}
				}
				if (hasMaxHeight) {
					try {
						max = Integer.parseInt(maxHeightStr.trim());
					} catch (NumberFormatException e) {
						ContinuityClient.LOGGER.warn("Invalid 'maxHeight' value '" + minHeightStr + "' in file '" + resourceId + "' in pack '" + packId + "'");
						hasMaxHeight = false;
					}
				}

				int finalMin = min;
				int finalMax = max;
				if (hasMinHeight && hasMaxHeight) {
					if (finalMin < finalMax) {
						heightPredicate = y -> y >= finalMin && y <= finalMax;
					} else if (finalMin > finalMax) {
						heightPredicate = y -> y >= finalMax && y <= finalMin;
					} else {
						heightPredicate = y -> y == finalMin;
					}
				} else if (hasMinHeight) {
					heightPredicate = y -> y >= finalMin;
				} else if (hasMaxHeight) {
					heightPredicate = y -> y <= finalMax;
				}
			}
		}
	}

	protected void parseName() {
		String nameStr = properties.getProperty("name");
		if (nameStr == null) {
			return;
		}

		nameStr = nameStr.trim();

		boolean isPattern;
		boolean caseInsensitive;
		if (nameStr.startsWith("regex:")) {
			nameStr = nameStr.substring(6);
			isPattern = false;
			caseInsensitive = false;
		} else if (nameStr.startsWith("iregex:")) {
			nameStr = nameStr.substring(7);
			isPattern = false;
			caseInsensitive = true;
		} else if (nameStr.startsWith("pattern:")) {
			nameStr = nameStr.substring(8);
			isPattern = true;
			caseInsensitive = false;
		} else if (nameStr.startsWith("ipattern:")) {
			nameStr = nameStr.substring(9);
			isPattern = true;
			caseInsensitive = true;
		} else {
			blockEntityNamePredicate = nameStr::equals;
			return;
		}

		String patternStr = nameStr;
		if (isPattern) {
			patternStr = Pattern.quote(patternStr);
			patternStr = patternStr.replace("?", "\\E.\\Q");
			patternStr = patternStr.replace("*", "\\E.*\\Q");
		}
		Pattern pattern = Pattern.compile(patternStr, caseInsensitive ? Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE : 0);
		blockEntityNamePredicate = blockEntityName -> pattern.matcher(blockEntityName).matches();
	}

	protected void parsePrioritize() {
		String prioritizeStr = properties.getProperty("prioritize");
		if (prioritizeStr == null) {
			prioritized = matchTilesSet != null;
			return;
		}

		prioritized = Boolean.parseBoolean(prioritizeStr.trim());
	}

	protected void parseResourceCondition() {
		String conditionsStr = properties.getProperty("resourceCondition");
		if (conditionsStr == null) {
			return;
		}

		String[] conditionStrs = conditionsStr.trim().split("\\|");
		if (conditionStrs.length != 0) {
			for (int i = 0; i < conditionStrs.length; i++) {
				String conditionStr = conditionStrs[i];
				if (conditionStr.isEmpty()) {
					continue;
				}

				String[] parts = conditionStr.split("@", 2);
				if (parts.length != 0) {
					String resourceStr = parts[0];
					Identifier resourceId;
					try {
						resourceId = Identifier.parse(resourceStr);
					} catch (IdentifierException e) {
						ContinuityClient.LOGGER.warn("Invalid resource '" + resourceStr + "' in 'resourceCondition' element '" + conditionStr + "' at index " + i + " in file '" + this.resourceId + "' in pack '" + packId + "'", e);
						continue;
					}

					String packStr;
					if (parts.length > 1) {
						packStr = parts[1];
					} else {
						packStr = null;
					}

					if (packStr == null || packStr.equals("default")) {
						Optional<Resource> optionalResource = resourceManager.getResource(resourceId);
						if (optionalResource.isPresent() && !optionalResource.get().sourcePackId().equals("vanilla")) {
							valid = false;
							break;
						}
					} else if (packStr.equals("programmer_art")) {
						Optional<Resource> optionalResource = resourceManager.getResource(resourceId);
						if (optionalResource.isPresent() && !optionalResource.get().sourcePackId().equals("programmer_art")) {
							valid = false;
							break;
						}
					} else {
						ContinuityClient.LOGGER.warn("Unknown pack '" + packStr + "' in 'resourceCondition' element '" + conditionStr + "' at index " + i + " in file '" + this.resourceId + "' in pack '" + packId + "'");
					}
				} else {
					ContinuityClient.LOGGER.warn("Invalid 'resourceCondition' element '" + conditionStr + "' at index " + i + " in file '" + resourceId + "' in pack '" + packId + "'");
				}
			}
		}
	}

	protected boolean isValid() {
		return valid;
	}

	public Properties getProperties() {
		return properties;
	}

	public Identifier getResourceId() {
		return resourceId;
	}

	public String getPackId() {
		return packId;
	}

	public int getPackPriority() {
		return packPriority;
	}

	public String getMethod() {
		return method;
	}

	@Nullable
	public Set<Identifier> getMatchTilesSet() {
		return matchTilesSet;
	}

	@Nullable
	public Predicate<BlockState> getMatchBlocksPredicate() {
		return matchBlocksPredicate;
	}

	public List<Identifier> getSpriteIds() {
		return spriteIds;
	}

	@Nullable
	public EnumSet<Direction> getFaces() {
		return faces;
	}

	@Nullable
	public Predicate<Biome> getBiomePredicate() {
		return biomePredicate;
	}

	@Nullable
	public IntPredicate getHeightPredicate() {
		return heightPredicate;
	}

	@Nullable
	public Predicate<String> getBlockEntityNamePredicate() {
		return blockEntityNamePredicate;
	}

	public boolean isPrioritized() {
		return prioritized;
	}

	public static <T extends BaseCtmProperties> Factory<T> wrapFactory(Factory<T> factory) {
		return (properties, resourceId, pack, packPriority, resourceManager, method) -> {
			T ctmProperties = factory.createProperties(properties, resourceId, pack, packPriority, resourceManager, method);
			if (ctmProperties == null) {
				return null;
			}
			ctmProperties.init();
			if (ctmProperties.isValid()) {
				return ctmProperties;
			}
			return null;
		};
	}
}
