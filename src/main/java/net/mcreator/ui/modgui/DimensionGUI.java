/*
 * MCreator (https://mcreator.net/)
 * Copyright (C) 2020 Pylo and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.mcreator.ui.modgui;

import net.mcreator.blockly.data.Dependency;
import net.mcreator.element.parts.Particle;
import net.mcreator.element.parts.TabEntry;
import net.mcreator.element.types.Dimension;
import net.mcreator.generator.GeneratorFlavor;
import net.mcreator.minecraft.DataListEntry;
import net.mcreator.minecraft.ElementUtil;
import net.mcreator.ui.MCreator;
import net.mcreator.ui.MCreatorApplication;
import net.mcreator.ui.component.JColor;
import net.mcreator.ui.component.JStringListField;
import net.mcreator.ui.component.TranslatedComboBox;
import net.mcreator.ui.component.util.ComboBoxUtil;
import net.mcreator.ui.component.util.ComponentUtils;
import net.mcreator.ui.component.util.PanelUtils;
import net.mcreator.ui.dialogs.TypedTextureSelectorDialog;
import net.mcreator.ui.help.HelpUtils;
import net.mcreator.ui.init.L10N;
import net.mcreator.ui.init.UIRES;
import net.mcreator.ui.laf.renderer.ItemTexturesComboBoxRenderer;
import net.mcreator.ui.laf.themes.Theme;
import net.mcreator.ui.minecraft.*;
import net.mcreator.ui.procedure.AbstractProcedureSelector;
import net.mcreator.ui.procedure.ProcedureSelector;
import net.mcreator.ui.procedure.StringListProcedureSelector;
import net.mcreator.ui.validation.AggregatedValidationResult;
import net.mcreator.ui.validation.ValidationGroup;
import net.mcreator.ui.validation.component.VTextField;
import net.mcreator.ui.validation.validators.*;
import net.mcreator.ui.workspace.resources.TextureType;
import net.mcreator.util.StringUtils;
import net.mcreator.workspace.elements.ModElement;
import net.mcreator.workspace.elements.VariableTypeLoader;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

public class DimensionGUI extends ModElementGUI<Dimension> {

	private final VTextField igniterName = new VTextField(19);
	private final TranslatedComboBox igniterRarity = new TranslatedComboBox(
			//@formatter:off
			Map.entry("COMMON", "elementgui.common.rarity_common"),
			Map.entry("UNCOMMON", "elementgui.common.rarity_uncommon"),
			Map.entry("RARE", "elementgui.common.rarity_rare"),
			Map.entry("EPIC", "elementgui.common.rarity_epic")
			//@formatter:on
	);

	private StringListProcedureSelector specialInformation;

	private TextureSelectionButton portalTexture;
	private TextureSelectionButton texture;

	private MCItemHolder portalFrame;
	private MCItemHolder mainFillerBlock;
	private MCItemHolder fluidBlock;

	private final JCheckBox canRespawnHere = L10N.checkbox("elementgui.common.enable");
	private final JCheckBox hasFog = L10N.checkbox("elementgui.common.enable");
	private final JCheckBox isDark = L10N.checkbox("elementgui.common.enable");
	private final JCheckBox doesWaterVaporize = L10N.checkbox("elementgui.common.enable");
	private final JCheckBox hasSkyLight = L10N.checkbox("elementgui.common.enable");
	private final JCheckBox imitateOverworldBehaviour = L10N.checkbox("elementgui.common.enable");
	private final JSpinner coordinateScale = new JSpinner(new SpinnerNumberModel(1, 0.01, 1000, 0.01));
	private final VTextField infiniburnTag = new VTextField();
	private final JCheckBox hasFixedTime = L10N.checkbox("elementgui.common.enable");
	private final JSpinner fixedTimeValue = new JSpinner(new SpinnerNumberModel(0, 0, 24000, 1));

	private final JCheckBox enablePortal = L10N.checkbox("elementgui.dimension.enable_portal");
	private final JCheckBox enableIgniter = L10N.checkbox("elementgui.common.enable");

	private final SoundSelector portalSound = new SoundSelector(mcreator);
	private final JColor airColor = new JColor(mcreator, true, false);

	private final DataListComboBox portalParticles = new DataListComboBox(mcreator);

	private final JComboBox<String> worldGenType = new JComboBox<>(
			new String[] { "Normal world gen", "Nether like gen", "End like gen" });

	private final JComboBox<String> sleepResult = new JComboBox<>(new String[] { "ALLOW", "DENY", "BED_EXPLODES" });

	private BiomeListField biomesInDimension;

	private final TabListField creativeTabs = new TabListField(mcreator);

	private final JSpinner luminance = new JSpinner(new SpinnerNumberModel(0, 0, 15, 1));

	private ProcedureSelector portalMakeCondition;
	private ProcedureSelector portalUseCondition;

	private ProcedureSelector whenPortaTriggerlUsed;
	private ProcedureSelector onPortalTickUpdate;
	private ProcedureSelector onPlayerEntersDimension;
	private ProcedureSelector onPlayerLeavesDimension;

	private final ValidationGroup page1group = new ValidationGroup();
	private final ValidationGroup page2group = new ValidationGroup();

	public DimensionGUI(MCreator mcreator, ModElement modElement, boolean editingMode) {
		super(mcreator, modElement, editingMode);
		this.initGUI();
		super.finalizeGUI();
	}

	@Override protected void initGUI() {
		whenPortaTriggerlUsed = new ProcedureSelector(this.withEntry("dimension/when_portal_used"), mcreator,
				L10N.t("elementgui.dimension.event_portal_trigger_used"),
				VariableTypeLoader.BuiltInTypes.ACTIONRESULTTYPE, Dependency.fromString(
				"x:number/y:number/z:number/world:world/entity:entity/itemstack:itemstack")).makeReturnValueOptional();
		onPortalTickUpdate = new ProcedureSelector(this.withEntry("dimension/on_portal_tick_update"), mcreator,
				L10N.t("elementgui.dimension.event_portal_tick_update"),
				Dependency.fromString("x:number/y:number/z:number/world:world/blockstate:blockstate"));
		onPlayerEntersDimension = new ProcedureSelector(this.withEntry("dimension/when_player_enters"), mcreator,
				L10N.t("elementgui.dimension.event_player_enters"),
				Dependency.fromString("x:number/y:number/z:number/world:world/entity:entity"));
		onPlayerLeavesDimension = new ProcedureSelector(this.withEntry("dimension/when_player_leaves"), mcreator,
				L10N.t("elementgui.dimension.event_player_leaves"),
				Dependency.fromString("x:number/y:number/z:number/world:world/entity:entity"));

		portalMakeCondition = new ProcedureSelector(this.withEntry("dimension/condition_portal_make"), mcreator,
				L10N.t("elementgui.dimension.event_can_make_portal"), VariableTypeLoader.BuiltInTypes.LOGIC,
				Dependency.fromString(
						"x:number/y:number/z:number/world:world/entity:entity/itemstack:itemstack")).makeInline();
		portalUseCondition = new ProcedureSelector(this.withEntry("dimension/condition_portal_use"), mcreator,
				L10N.t("elementgui.dimension.event_can_travel_through_portal"), VariableTypeLoader.BuiltInTypes.LOGIC,
				Dependency.fromString("x:number/y:number/z:number/world:world/entity:entity")).makeInline();
		specialInformation = new StringListProcedureSelector(this.withEntry("item/special_information"), mcreator,
				L10N.t("elementgui.common.special_information"), AbstractProcedureSelector.Side.CLIENT,
				new JStringListField(mcreator, null), 0,
				Dependency.fromString("x:number/y:number/z:number/entity:entity/world:world/itemstack:itemstack"));

		worldGenType.setRenderer(new ItemTexturesComboBoxRenderer());
		biomesInDimension = new BiomeListField(mcreator);

		portalParticles.setPrototypeDisplayValue(new DataListEntry.Dummy("XXXXXXXXXXXXXXXXXXX"));

		portalFrame = new MCItemHolder(mcreator, ElementUtil::loadBlocks);
		mainFillerBlock = new MCItemHolder(mcreator, ElementUtil::loadBlocks);
		fluidBlock = new MCItemHolder(mcreator, ElementUtil::loadBlocks);

		JPanel propertiesPage = new JPanel(new BorderLayout(10, 10));
		JPanel generationPage = new JPanel(new BorderLayout(10, 10));
		JPanel pane2 = new JPanel(new BorderLayout(10, 10));
		JPanel pane5 = new JPanel(new BorderLayout(10, 10));

		// Dimension type settings
		JPanel dimensionTypeSettings = new JPanel(new GridLayout(10, 2, 15, 2));
		dimensionTypeSettings.setOpaque(false);

		sleepResult.setPreferredSize(new java.awt.Dimension(0, 42));

		dimensionTypeSettings.add(HelpUtils.wrapWithHelpButton(this.withEntry("dimension/sleep_result"),
				L10N.label("elementgui.dimension.sleep_result")));
		dimensionTypeSettings.add(sleepResult);

		dimensionTypeSettings.add(HelpUtils.wrapWithHelpButton(this.withEntry("dimension/imitate_overworld"),
				L10N.label("elementgui.dimension.imitate_overworld_behaviour")));
		dimensionTypeSettings.add(imitateOverworldBehaviour);

		dimensionTypeSettings.add(HelpUtils.wrapWithHelpButton(this.withEntry("dimension/can_use_respawn_anchor"),
				L10N.label("elementgui.dimension.can_use_respawn_anchor")));
		dimensionTypeSettings.add(canRespawnHere);

		dimensionTypeSettings.add(HelpUtils.wrapWithHelpButton(this.withEntry("dimension/has_skylight"),
				L10N.label("elementgui.dimension.has_sky_light")));
		dimensionTypeSettings.add(hasSkyLight);

		dimensionTypeSettings.add(HelpUtils.wrapWithHelpButton(this.withEntry("dimension/is_dark"),
				L10N.label("elementgui.dimension.is_dark")));
		dimensionTypeSettings.add(isDark);

		dimensionTypeSettings.add(HelpUtils.wrapWithHelpButton(this.withEntry("dimension/does_water_vaporize"),
				L10N.label("elementgui.dimension.does_water_vaporize")));
		dimensionTypeSettings.add(doesWaterVaporize);

		dimensionTypeSettings.add(HelpUtils.wrapWithHelpButton(this.withEntry("dimension/has_fixed_time"),
				L10N.label("elementgui.dimension.has_fixed_time")));
		dimensionTypeSettings.add(hasFixedTime);

		dimensionTypeSettings.add(HelpUtils.wrapWithHelpButton(this.withEntry("dimension/fixed_time_value"),
				L10N.label("elementgui.dimension.fixed_time_value")));
		dimensionTypeSettings.add(fixedTimeValue);

		dimensionTypeSettings.add(HelpUtils.wrapWithHelpButton(this.withEntry("dimension/coordinate_scale"),
				L10N.label("elementgui.dimension.coordinate_scale")));
		dimensionTypeSettings.add(coordinateScale);

		dimensionTypeSettings.add(HelpUtils.wrapWithHelpButton(this.withEntry("dimension/infiniburn_tag"),
				L10N.label("elementgui.dimension.infiniburn_tag")));
		dimensionTypeSettings.add(infiniburnTag);

		dimensionTypeSettings.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Theme.current().getForegroundColor(), 1),
				L10N.t("elementgui.dimension.dimension_type_settings"), TitledBorder.LEADING,
				TitledBorder.DEFAULT_POSITION, getFont().deriveFont(12.0f), Theme.current().getForegroundColor()));

		JPanel dimensionEffects = new JPanel(new GridLayout(2, 2, 15, 5));
		dimensionEffects.setOpaque(false);

		dimensionEffects.add(HelpUtils.wrapWithHelpButton(this.withEntry("dimension/fog_color"),
				L10N.label("elementgui.dimension.fog_air_color")));
		dimensionEffects.add(airColor);

		dimensionEffects.add(HelpUtils.wrapWithHelpButton(this.withEntry("dimension/has_fog"),
				L10N.label("elementgui.dimension.has_fog")));
		dimensionEffects.add(hasFog);

		dimensionEffects.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Theme.current().getForegroundColor(), 1),
				L10N.t("elementgui.dimension.dimension_effects"), TitledBorder.LEADING,
				TitledBorder.DEFAULT_POSITION, getFont().deriveFont(12.0f), Theme.current().getForegroundColor()));

		isDark.setOpaque(false);
		hasSkyLight.setOpaque(false);
		imitateOverworldBehaviour.setOpaque(false);
		canRespawnHere.setOpaque(false);
		doesWaterVaporize.setOpaque(false);
		hasFixedTime.setOpaque(false);
		hasFixedTime.addActionListener(e -> fixedTimeValue.setEnabled(hasFixedTime.isSelected()));
		fixedTimeValue.setEnabled(false);
		if (!isEditingMode()) {
			infiniburnTag.setText("minecraft:infiniburn_overworld");
		}

		airColor.setOpaque(false);
		airColor.setPreferredSize(new java.awt.Dimension(240, 42));
		hasFog.setOpaque(false);

		propertiesPage.add("Center", PanelUtils.totalCenterInPanel(
				PanelUtils.westAndEastElement(dimensionTypeSettings, PanelUtils.pullElementUp(dimensionEffects))));
		propertiesPage.setOpaque(false);

		// Dimension generation settings
		JPanel insid = new JPanel(new BorderLayout(20, 20));

		insid.add("East", PanelUtils.northAndCenterElement(
				PanelUtils.join(FlowLayout.LEFT, L10N.label("elementgui.dimension.world_gen_type"), worldGenType),
				PanelUtils.join(new JLabel(UIRES.get("dimension_types")))));

		JPanel worldgenSettings = new JPanel(new GridLayout(3, 2, 3, 3));
		worldgenSettings.setOpaque(false);

		biomesInDimension.setPreferredSize(new java.awt.Dimension(300, 42));

		worldgenSettings.add(HelpUtils.wrapWithHelpButton(this.withEntry("dimension/main_filler_block"),
				L10N.label("elementgui.dimension.main_filler_block"), new Color(0x2980b9)));
		worldgenSettings.add(PanelUtils.join(mainFillerBlock));

		worldgenSettings.add(HelpUtils.wrapWithHelpButton(this.withEntry("dimension/fluid_block"),
				L10N.label("elementgui.dimension.fluid_block"), new Color(0xB8E700)));
		worldgenSettings.add(PanelUtils.join(fluidBlock));

		worldgenSettings.add(HelpUtils.wrapWithHelpButton(this.withEntry("dimension/biomes"),
				L10N.label("elementgui.dimension.biomes_in")));
		worldgenSettings.add(biomesInDimension);

		insid.setOpaque(false);

		insid.add("Center", PanelUtils.totalCenterInPanel(worldgenSettings));
		generationPage.add("Center", PanelUtils.totalCenterInPanel(insid));

		generationPage.setOpaque(false);

		portalTexture = new TextureSelectionButton(new TypedTextureSelectorDialog(mcreator, TextureType.BLOCK));
		texture = new TextureSelectionButton(new TypedTextureSelectorDialog(mcreator, TextureType.ITEM));

		portalTexture.setOpaque(false);
		texture.setOpaque(false);
		enablePortal.setOpaque(false);
		enableIgniter.setOpaque(false);

		// Currently only Java based mods support dimension portals
		enablePortal.setSelected(modElement.getGeneratorConfiguration().getGeneratorFlavor().getBaseLanguage()
				== GeneratorFlavor.BaseLanguage.JAVA);
		enableIgniter.setSelected(modElement.getGeneratorConfiguration().getGeneratorFlavor().getBaseLanguage()
				== GeneratorFlavor.BaseLanguage.JAVA);

		JPanel proper = new JPanel(new GridLayout(4, 2, 5, 2));
		proper.setOpaque(false);

		JPanel proper22 = new JPanel(new GridLayout(3, 2, 5, 2));
		proper22.setOpaque(false);

		proper.add(HelpUtils.wrapWithHelpButton(this.withEntry("dimension/portal_particles"),
				L10N.label("elementgui.dimension.portal_particles")));
		proper.add(portalParticles);

		proper.add(HelpUtils.wrapWithHelpButton(this.withEntry("dimension/portal_sound"),
				L10N.label("elementgui.dimension.portal_sound")));
		proper.add(portalSound);

		proper.add(HelpUtils.wrapWithHelpButton(this.withEntry("block/luminance"),
				L10N.label("elementgui.dimension.portal_luminance")));
		proper.add(luminance);

		proper.add(HelpUtils.wrapWithHelpButton(this.withEntry("dimension/portal_frame_block"),
				L10N.label("elementgui.dimension.portal_frame_block")));
		proper.add(PanelUtils.join(portalFrame));

		proper22.add(HelpUtils.wrapWithHelpButton(this.withEntry("common/gui_name"),
				L10N.label("elementgui.dimension.portal_igniter_name")));
		proper22.add(igniterName);

		proper22.add(
				HelpUtils.wrapWithHelpButton(this.withEntry("item/rarity"), L10N.label("elementgui.common.rarity")));
		proper22.add(igniterRarity);

		proper22.add(HelpUtils.wrapWithHelpButton(this.withEntry("common/creative_tabs"),
				L10N.label("elementgui.dimension.portal_igniter_tabs")));
		proper22.add(creativeTabs);

		creativeTabs.setPreferredSize(new java.awt.Dimension(0, 42));

		portalSound.setText("block.portal.ambient");

		portalParticles.setFont(portalParticles.getFont().deriveFont(16.0f));

		JPanel igniterPanel = new JPanel(new BorderLayout(5, 5));
		igniterPanel.setOpaque(false);

		igniterPanel.add("North", PanelUtils.gridElements(1, 2, 5, 2,
				HelpUtils.wrapWithHelpButton(this.withEntry("dimension/enable_igniter"),
						L10N.label("elementgui.dimension.enable_new_igniter")), enableIgniter));

		igniterPanel.add("Center", PanelUtils.gridElements(1, 2,
				HelpUtils.wrapWithHelpButton(this.withEntry("dimension/portal_igniter_texture"),
						L10N.label("elementgui.dimension.portal_igniter_texture")), PanelUtils.join(texture)));

		JPanel conditions = new JPanel(new GridLayout(2, 1, 5, 2));
		conditions.setOpaque(false);
		conditions.add(specialInformation);
		conditions.add(portalMakeCondition);

		igniterPanel.add("South", PanelUtils.centerAndSouthElement(proper22, conditions, 2, 2));

		igniterPanel.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Theme.current().getForegroundColor(), 1),
				L10N.t("elementgui.dimension.portal_igniter_properties"), 0, 0, getFont().deriveFont(12.0f),
				Theme.current().getForegroundColor()));

		JPanel propertiesPanel = new JPanel(new BorderLayout(5, 2));
		propertiesPanel.setOpaque(false);
		propertiesPanel.add("Center", PanelUtils.totalCenterInPanel(PanelUtils.northAndCenterElement(
				PanelUtils.gridElements(1, 2, HelpUtils.wrapWithHelpButton(this.withEntry("dimension/portal_texture"),
						L10N.label("elementgui.dimension.portal_block_texture")), PanelUtils.join(portalTexture)),
				proper)));
		propertiesPanel.add("South", portalUseCondition);

		propertiesPanel.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Theme.current().getForegroundColor(), 1),
				L10N.t("elementgui.dimension.portal_properties"), 0, 0, getFont().deriveFont(12.0f),
				Theme.current().getForegroundColor()));

		JPanel portalPanelMain = new JPanel(new BorderLayout(0, 0));
		portalPanelMain.setOpaque(false);
		portalPanelMain.add("West", propertiesPanel);
		portalPanelMain.add("East", PanelUtils.pullElementUp(igniterPanel));

		JPanel portalPanel = new JPanel(new BorderLayout(5, 2));
		portalPanel.setOpaque(false);
		portalPanel.add("North", PanelUtils.join(FlowLayout.LEFT,
				HelpUtils.wrapWithHelpButton(this.withEntry("dimension/enable_portal"),
						L10N.label("elementgui.dimension.enable_dimension_portal")), enablePortal));
		portalPanel.add("Center", portalPanelMain);

		pane2.setOpaque(false);

		pane2.add(PanelUtils.totalCenterInPanel(portalPanel));

		ComponentUtils.deriveFont(igniterName, 16);

		enablePortal.addActionListener(e -> updatePortalElements());
		enableIgniter.addActionListener(e -> updateIgniterElements(enableIgniter.isSelected()));

		JPanel events = new JPanel(new GridLayout(1, 4, 5, 5));
		events.add(whenPortaTriggerlUsed);
		events.add(onPortalTickUpdate);
		events.add(onPlayerEntersDimension);
		events.add(onPlayerLeavesDimension);
		events.setOpaque(false);
		pane5.add(PanelUtils.totalCenterInPanel(events));
		pane5.setOpaque(false);

		infiniburnTag.setValidator(new ResourceLocationValidator<>(L10N.t("elementgui.dimension.infiniburn_validator"),
				infiniburnTag, true));
		infiniburnTag.enableRealtimeValidation();

		igniterName.setValidator(new ConditionalTextFieldValidator(igniterName,
				L10N.t("elementgui.dimension.error_portal_igniter_needs_name"), enableIgniter, true));
		portalTexture.setValidator(new TileHolderValidator(portalTexture, enablePortal));
		texture.setValidator(new TileHolderValidator(texture, enableIgniter));
		portalFrame.setValidator(new MCItemHolderValidator(portalFrame, enablePortal));
		igniterName.enableRealtimeValidation();

		page1group.addValidationElement(igniterName);
		page1group.addValidationElement(portalTexture);
		page1group.addValidationElement(texture);
		page1group.addValidationElement(portalFrame);

		biomesInDimension.setValidator(
				new ItemListFieldValidator(biomesInDimension, L10N.t("elementgui.dimension.error_select_biome")));
		mainFillerBlock.setValidator(new MCItemHolderValidator(mainFillerBlock).considerAirAsEmpty());
		fluidBlock.setValidator(new MCItemHolderValidator(fluidBlock));

		page2group.addValidationElement(biomesInDimension);
		page2group.addValidationElement(mainFillerBlock);
		page2group.addValidationElement(fluidBlock);

		addPage(L10N.t("elementgui.dimension.page_generation"), generationPage);
		addPage(L10N.t("elementgui.common.page_properties"), propertiesPage);
		addPage(L10N.t("elementgui.dimension.page_portal"), pane2);
		addPage(L10N.t("elementgui.common.page_triggers"), pane5);

		if (!isEditingMode()) {
			creativeTabs.setListElements(List.of(new TabEntry(mcreator.getWorkspace(), "TOOLS")));

			String readableNameFromModElement = StringUtils.machineToReadableName(modElement.getName());
			igniterName.setText(readableNameFromModElement + " Portal Igniter");
		}
	}

	private void updatePortalElements() {
		portalFrame.setEnabled(enablePortal.isSelected());
		portalParticles.setEnabled(enablePortal.isSelected());
		portalSound.setEnabled(enablePortal.isSelected());
		luminance.setEnabled(enablePortal.isSelected());
		portalTexture.setEnabled(enablePortal.isSelected());
		portalUseCondition.setEnabled(enablePortal.isSelected());
		enableIgniter.setEnabled(enablePortal.isSelected());
		updateIgniterElements(enablePortal.isSelected() && enableIgniter.isSelected());
	}

	private void updateIgniterElements(boolean enabled) {
		igniterName.setEnabled(enabled);
		creativeTabs.setEnabled(enabled);
		specialInformation.setEnabled(enabled);
		texture.setEnabled(enabled);
		portalMakeCondition.setEnabled(enabled);
		igniterRarity.setEnabled(enabled);
	}

	@Override public void reloadDataLists() {
		super.reloadDataLists();
		whenPortaTriggerlUsed.refreshListKeepSelected();
		onPortalTickUpdate.refreshListKeepSelected();
		onPlayerEntersDimension.refreshListKeepSelected();
		onPlayerLeavesDimension.refreshListKeepSelected();

		portalMakeCondition.refreshListKeepSelected();
		portalUseCondition.refreshListKeepSelected();
		specialInformation.refreshListKeepSelected();

		ComboBoxUtil.updateComboBoxContents(portalParticles, ElementUtil.loadAllParticles(mcreator.getWorkspace()),
				new DataListEntry.Dummy("PORTAL"));
	}

	@Override protected AggregatedValidationResult validatePage(int page) {
		if (page == 0)
			return new AggregatedValidationResult(page2group);
		else if (page == 1)
			return new AggregatedValidationResult(infiniburnTag);
		else if (page == 2)
			return new AggregatedValidationResult(page1group);
		return new AggregatedValidationResult.PASS();
	}

	@Override public void openInEditingMode(Dimension dimension) {
		portalFrame.setBlock(dimension.portalFrame);
		mainFillerBlock.setBlock(dimension.mainFillerBlock);
		fluidBlock.setBlock(dimension.fluidBlock);
		portalSound.setSound(dimension.portalSound);
		enableIgniter.setSelected(dimension.enableIgniter);
		igniterName.setText(dimension.igniterName);
		igniterRarity.setSelectedItem(dimension.igniterRarity);
		specialInformation.setSelectedProcedure(dimension.specialInformation);
		portalTexture.setTexture(dimension.portalTexture);
		texture.setTexture(dimension.texture);
		worldGenType.setSelectedItem(dimension.worldGenType);
		sleepResult.setSelectedItem(dimension.sleepResult);
		creativeTabs.setListElements(dimension.creativeTabs);
		portalParticles.setSelectedItem(dimension.portalParticles);
		biomesInDimension.setListElements(dimension.biomesInDimension);
		airColor.setColor(dimension.airColor);
		canRespawnHere.setSelected(dimension.canRespawnHere);
		hasFog.setSelected(dimension.hasFog);
		isDark.setSelected(dimension.isDark);
		doesWaterVaporize.setSelected(dimension.doesWaterVaporize);
		imitateOverworldBehaviour.setSelected(dimension.imitateOverworldBehaviour);
		hasSkyLight.setSelected(dimension.hasSkyLight);
		hasFixedTime.setSelected(dimension.hasFixedTime);
		fixedTimeValue.setValue(dimension.fixedTimeValue);
		coordinateScale.setValue(dimension.coordinateScale);
		infiniburnTag.setText(dimension.infiniburnTag);
		enablePortal.setSelected(dimension.enablePortal);
		whenPortaTriggerlUsed.setSelectedProcedure(dimension.whenPortaTriggerlUsed);
		onPortalTickUpdate.setSelectedProcedure(dimension.onPortalTickUpdate);
		onPlayerEntersDimension.setSelectedProcedure(dimension.onPlayerEntersDimension);
		onPlayerLeavesDimension.setSelectedProcedure(dimension.onPlayerLeavesDimension);
		luminance.setValue(dimension.portalLuminance);
		portalMakeCondition.setSelectedProcedure(dimension.portalMakeCondition);
		portalUseCondition.setSelectedProcedure(dimension.portalUseCondition);

		fixedTimeValue.setEnabled(dimension.hasFixedTime);
		updatePortalElements();
	}

	@Override public Dimension getElementFromGUI() {
		Dimension dimension = new Dimension(modElement);
		dimension.texture = texture.getTextureHolder();
		dimension.portalTexture = portalTexture.getTextureHolder();
		dimension.portalParticles = new Particle(mcreator.getWorkspace(), portalParticles.getSelectedItem());
		dimension.creativeTabs = creativeTabs.getListElements();
		dimension.portalSound = portalSound.getSound();
		dimension.biomesInDimension = biomesInDimension.getListElements();
		dimension.airColor = airColor.getColor();
		dimension.canRespawnHere = canRespawnHere.isSelected();
		dimension.hasFog = hasFog.isSelected();
		dimension.isDark = isDark.isSelected();
		dimension.imitateOverworldBehaviour = imitateOverworldBehaviour.isSelected();
		dimension.hasSkyLight = hasSkyLight.isSelected();
		dimension.hasFixedTime = hasFixedTime.isSelected();
		dimension.fixedTimeValue = (int) fixedTimeValue.getValue();
		dimension.coordinateScale = (double) coordinateScale.getValue();
		dimension.infiniburnTag = infiniburnTag.getText();
		dimension.enablePortal = enablePortal.isSelected();
		dimension.portalFrame = portalFrame.getBlock();
		dimension.enableIgniter = enableIgniter.isSelected();
		dimension.igniterName = igniterName.getText();
		dimension.igniterRarity = igniterRarity.getSelectedItem();
		dimension.specialInformation = specialInformation.getSelectedProcedure();
		dimension.worldGenType = (String) worldGenType.getSelectedItem();
		dimension.sleepResult = (String) sleepResult.getSelectedItem();
		dimension.mainFillerBlock = mainFillerBlock.getBlock();
		dimension.fluidBlock = fluidBlock.getBlock();
		dimension.whenPortaTriggerlUsed = whenPortaTriggerlUsed.getSelectedProcedure();
		dimension.onPortalTickUpdate = onPortalTickUpdate.getSelectedProcedure();
		dimension.onPlayerEntersDimension = onPlayerEntersDimension.getSelectedProcedure();
		dimension.onPlayerLeavesDimension = onPlayerLeavesDimension.getSelectedProcedure();
		dimension.portalLuminance = (int) luminance.getValue();
		dimension.doesWaterVaporize = doesWaterVaporize.isSelected();
		dimension.portalMakeCondition = portalMakeCondition.getSelectedProcedure();
		dimension.portalUseCondition = portalUseCondition.getSelectedProcedure();
		return dimension;
	}

	@Override public @Nullable URI contextURL() throws URISyntaxException {
		return new URI(MCreatorApplication.SERVER_DOMAIN + "/wiki/how-make-dimension");
	}

}
