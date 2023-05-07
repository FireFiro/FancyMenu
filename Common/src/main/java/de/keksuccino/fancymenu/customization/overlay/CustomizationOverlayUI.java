package de.keksuccino.fancymenu.customization.overlay;

import java.awt.Color;
import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

import com.google.common.io.Files;
import com.mojang.blaze3d.systems.RenderSystem;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.setupsharing.SetupSharingHandler;
import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.event.acara.EventListener;
import de.keksuccino.fancymenu.event.events.screen.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.customization.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.event.events.ButtonCacheUpdatedEvent;
import de.keksuccino.fancymenu.customization.button.ButtonData;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.gameintro.GameIntroScreen;
import de.keksuccino.fancymenu.customization.guicreator.CustomGuiBase;
import de.keksuccino.fancymenu.customization.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.customization.guicreator.CreateCustomGuiPopup;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.rendering.ui.contextmenu.ContextMenu;
import de.keksuccino.fancymenu.rendering.ui.MenuBar;
import de.keksuccino.fancymenu.rendering.ui.UIBase;
import de.keksuccino.fancymenu.rendering.ui.MenuBar.ElementAlignment;
import de.keksuccino.fancymenu.rendering.ui.popup.FMNotificationPopup;
import de.keksuccino.fancymenu.rendering.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.rendering.ui.popup.FMYesNoPopup;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.AdvancedImageButton;
import de.keksuccino.konkrete.gui.screens.ConfigScreen;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.gui.screens.popup.TextInputPopup;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSerializer;
import de.keksuccino.konkrete.properties.PropertiesSet;
import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.progress.StoringChunkProgressListener;

public class CustomizationOverlayUI extends UIBase {

	protected static final ResourceLocation CLOSE_BUTTON_TEXTURE = new ResourceLocation("keksuccino", "close_btn.png");
	protected static final ResourceLocation RELOAD_BUTTON_TEXTURE = new ResourceLocation("keksuccino", "/filechooser/back_icon.png");

	protected static final Color BUTTON_INFO_BACKGROUND_COLOR = new Color(102, 0, 102, 200);
	protected static final Color WARNING_COLOR = new Color(230, 15, 0, 240);
	protected static final Color MENU_INFO_BACKGROUND_COLOR = new Color(0, 0, 0, 240);

	public static MenuBar bar;

	public static boolean showButtonInfo = false;
	public static boolean showMenuInfo = false;
	protected static List<ButtonData> buttons = new ArrayList<>();
	protected static int tick = 0;
	protected static long lastButtonInfoRightClick = 0;

	public static void init() {

		EventHandler.INSTANCE.registerListenersOf(new CustomizationOverlayUI());

	}

	public static void updateUI() {
		try {

			boolean extended = true;
			if (bar != null) {
				extended = bar.isExtended();
			}

			bar = new MenuBar();

			/** CURRENT MENU TAB START **/
			ContextMenu currentMenu = new ContextMenu();
			currentMenu.setAutoclose(true);
			bar.addChild(currentMenu, "fm.ui.tab.current", ElementAlignment.LEFT);

			String toggleLabel = Locals.localize("helper.popup.togglecustomization.enable");
			if (ScreenCustomization.isCustomizationEnabledForScreen(Minecraft.getInstance().screen)) {
				toggleLabel = Locals.localize("helper.popup.togglecustomization.disable");
			}
			OverlayButton toggleCustomizationButton = new OverlayButton(0, 0, 0, 0, toggleLabel, true, (press) -> {
				if (ScreenCustomization.isCustomizationEnabledForScreen(Minecraft.getInstance().screen)) {
					press.setMessage(Component.literal(Locals.localize("helper.popup.togglecustomization.enable")));
					ScreenCustomization.disableCustomizationForScreen(Minecraft.getInstance().screen);
					ScreenCustomization.reloadFancyMenu();
				} else {
					press.setMessage(Component.literal(Locals.localize("helper.popup.togglecustomization.disable")));
					ScreenCustomization.enableCustomizationForScreen(Minecraft.getInstance().screen);
					ScreenCustomization.reloadFancyMenu();
				}
			});
			toggleCustomizationButton.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.customization.onoff.btndesc"), "%n%"));
			currentMenu.addContent(toggleCustomizationButton);

			ContextMenu layoutsMenu = new ContextMenu();
			layoutsMenu.setAutoclose(true);
			currentMenu.addChild(layoutsMenu);

			OverlayButton newLayoutButton = new OverlayButton(0, 0, 0, 0, Locals.localize("helper.ui.current.layouts.new"), true, (press) -> {
				LayoutEditorScreen.isActive = true;
				Screen s = Minecraft.getInstance().screen;
				Minecraft.getInstance().setScreen(new LayoutEditorScreen(s));
				ScreenCustomization.stopSounds();
				ScreenCustomization.resetSounds();
				for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
					if (r instanceof AdvancedAnimation) {
						((AdvancedAnimation)r).stopAudio();
						if (((AdvancedAnimation)r).replayIntro()) {
							r.resetAnimation();
						}
					}
				}
			});
			newLayoutButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.current.layouts.new.desc"), "%n%"));
			layoutsMenu.addContent(newLayoutButton);

			ManageLayoutsContextMenu manageLayoutsMenu = new ManageLayoutsContextMenu(false);
			manageLayoutsMenu.setAutoclose(true);
			layoutsMenu.addChild(manageLayoutsMenu);

			OverlayButton manageLayoutsButton = new OverlayButton(0, 0, 0, 0, Locals.localize("helper.ui.current.layouts.manage"), true, (press) -> {
				manageLayoutsMenu.setParentButton((AdvancedButton) press);
				manageLayoutsMenu.openMenuAt(press);
			});
			manageLayoutsButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.current.layouts.manage.desc"), "%n%"));
			layoutsMenu.addContent(manageLayoutsButton);

			OverlayButton layoutsButton = new OverlayButton(0, 0, 0, 0, Locals.localize("helper.ui.current.layouts"), true, (press) -> {
				layoutsMenu.setParentButton((AdvancedButton) press);
				layoutsMenu.openMenuAt(0, press.y);
			});
			if (!ScreenCustomization.isCustomizationEnabledForScreen(Minecraft.getInstance().screen)) {
				layoutsButton.active = false;
			}
			layoutsButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.current.layouts.desc"), "%n%"));
			currentMenu.addContent(layoutsButton);

			ContextMenu advancedMenu = new ContextMenu();
			advancedMenu.setAutoclose(true);
			currentMenu.addChild(advancedMenu);

			OverrideMenuContextMenu overrideMenu = new OverrideMenuContextMenu();
			overrideMenu.setAutoclose(true);
			advancedMenu.addChild(overrideMenu);

			String overrLabel = Locals.localize("helper.buttons.tools.overridemenu");
			if (ScreenCustomization.isOverridingOtherScreen(Minecraft.getInstance().screen)) {
				overrLabel = Locals.localize("helper.buttons.tools.resetoverride");
			}
			OverlayButton overrideButton = new OverlayButton(0, 0, 0, 0, overrLabel, true, (press) -> {

				if (!ScreenCustomization.isOverridingOtherScreen(Minecraft.getInstance().screen)) {

					overrideMenu.setParentButton((AdvancedButton) press);
					overrideMenu.openMenuAt(0, press.y);

				} else {

					for (String s : FileUtils.getFiles(FancyMenu.getCustomizationsDirectory().getPath())) {
						PropertiesSet props = PropertiesSerializer.getProperties(s);
						if (props == null) {
							continue;
						}
						PropertiesSet props2 = new PropertiesSet(props.getPropertiesType());
						List<PropertiesSection> l = props.getProperties();
						List<PropertiesSection> l2 = new ArrayList<>();
						boolean b = false;

						List<PropertiesSection> metas = props.getPropertiesOfType("customization-meta");
						if ((metas == null) || metas.isEmpty()) {
							metas = props.getPropertiesOfType("type-meta");
						}
						if (metas != null) {
							if (metas.isEmpty()) {
								continue;
							}
							String identifier = metas.get(0).getEntryValue("identifier");
							Screen overridden = ((CustomGuiBase)Minecraft.getInstance().screen).getOverriddenScreen();
							if ((identifier == null) || !identifier.equalsIgnoreCase(overridden.getClass().getName())) {
								continue;
							}

						} else {
							continue;
						}

						for (PropertiesSection sec : l) {
							String action = sec.getEntryValue("action");
							if (sec.getSectionType().equalsIgnoreCase("customization-meta") || sec.getSectionType().equalsIgnoreCase("type-meta")) {
								l2.add(sec);
								continue;
							}
							if ((action != null) && !action.equalsIgnoreCase("overridemenu")) {
								l2.add(sec);
							}
							if ((action != null) && action.equalsIgnoreCase("overridemenu")) {
								b = true;
							}
						}

						if (b) {
							File f = new File(s);
							if (f.exists() && f.isFile()) {
								f.delete();
							}

							if (l2.size() > 1) {
								for (PropertiesSection sec : l2) {
									props2.addProperties(sec);
								}

								PropertiesSerializer.writeProperties(props2, s);
							}
						}
					}

					ScreenCustomization.reloadFancyMenu();
					if (Minecraft.getInstance().screen instanceof CustomGuiBase) {
						Minecraft.getInstance().setScreen(((CustomGuiBase) Minecraft.getInstance().screen).getOverriddenScreen());
					}
				}
			});
			overrideButton.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.customization.overridewith.btndesc"), "%n%"));
			if (!(Minecraft.getInstance().screen instanceof CustomGuiBase)) {
				advancedMenu.addContent(overrideButton);
			} else if (((CustomGuiBase)Minecraft.getInstance().screen).getOverriddenScreen() != null) {
				advancedMenu.addContent(overrideButton);
			}

			OverlayButton advancedButton = new OverlayButton(0, 0, 0, 0, Locals.localize("helper.ui.current.advanced"), true, (press) -> {
				advancedMenu.setParentButton((AdvancedButton) press);
				advancedMenu.openMenuAt(0, press.y);
			});
			if (!ScreenCustomization.isCustomizationEnabledForScreen(Minecraft.getInstance().screen)) {
				advancedButton.active = false;
			}
			advancedButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.current.advanced.desc"), "%n%"));
			if (FancyMenu.getConfig().getOrDefault("advancedmode", false)) {
				currentMenu.addContent(advancedButton);
			}

			OverlayButton closeCustomGuiButton = new OverlayButton(0, 0, 0, 0, Locals.localize("helper.ui.misc.closegui"), true, (press) -> {
				Minecraft.getInstance().setScreen(null);
			});
			closeCustomGuiButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.misc.closegui.desc"), "%n%"));
			if ((Minecraft.getInstance().screen instanceof CustomGuiBase) && (((CustomGuiBase)Minecraft.getInstance().screen).getOverriddenScreen() == null)) {
				currentMenu.addContent(closeCustomGuiButton);
			}

			OverlayButton currentTab = new OverlayButton(0, 0, 0, 0, Locals.localize("helper.ui.current"), true, (press) -> {
				currentMenu.setParentButton((AdvancedButton) press);
				currentMenu.openMenuAt(press.x, press.y + press.getHeight());
			});
			bar.addElement(currentTab, "fm.ui.tab.current", ElementAlignment.LEFT, false);
			/** CURRENT MENU TAB END **/

			/** UNIVERSAL LAYOUTS START **/
			ContextMenu universalLayoutsMenu = new ContextMenu();
			universalLayoutsMenu.setAutoclose(true);
			bar.addChild(universalLayoutsMenu, "fm.ui.tab.universal_layouts", ElementAlignment.LEFT);

			OverlayButton newUniversalLayoutButton = new OverlayButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.ui.universal_layouts.new"), true, (press) -> {
				LayoutEditorScreen.isActive = true;
				Minecraft.getInstance().setScreen(new LayoutEditorScreen(new CustomGuiBase("", "%fancymenu:universal_layout%", true, Minecraft.getInstance().screen, null)));
				ScreenCustomization.stopSounds();
				ScreenCustomization.resetSounds();
				for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
					if (r instanceof AdvancedAnimation) {
						((AdvancedAnimation)r).stopAudio();
						if (((AdvancedAnimation)r).replayIntro()) {
							r.resetAnimation();
						}
					}
				}
			});
			universalLayoutsMenu.addContent(newUniversalLayoutButton);

			ManageLayoutsContextMenu manageUniversalLayoutsMenu = new ManageLayoutsContextMenu(true);
			manageUniversalLayoutsMenu.setAutoclose(true);
			universalLayoutsMenu.addChild(manageUniversalLayoutsMenu);

			OverlayButton manageUniversalLayoutsButton = new OverlayButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.ui.universal_layouts.manage"), true, (press) -> {
				manageUniversalLayoutsMenu.setParentButton((AdvancedButton) press);
				manageUniversalLayoutsMenu.openMenuAt(press);
			});
			universalLayoutsMenu.addContent(manageUniversalLayoutsButton);

			OverlayButton universalLayoutsTabButton = new OverlayButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.ui.universal_layouts"), true, (press) -> {
				universalLayoutsMenu.setParentButton((AdvancedButton) press);
				universalLayoutsMenu.openMenuAt(press.x, press.y + press.getHeight());
			});
			universalLayoutsTabButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.ui.universal_layouts.btn.desc"), "%n%"));
			bar.addElement(universalLayoutsTabButton, "fm.ui.tab.universal_layouts", ElementAlignment.LEFT, false);
			/** UNIVERSAL LAYOUTS END **/

			/** SETUP TAB START **/
			ContextMenu setupMenu = new ContextMenu();
			setupMenu.setAutoclose(true);
			bar.addChild(setupMenu, "fm.ui.tab.setup_import_export", ElementAlignment.LEFT);

			OverlayButton setupTab = new OverlayButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.ui.setup"), true, (press) -> {
				setupMenu.setParentButton((AdvancedButton) press);
				setupMenu.openMenuAt(press.x, press.y + press.getHeight());
			});
			bar.addElement(setupTab, "fm.ui.tab.setup_import_export", ElementAlignment.LEFT, false);

			//Export Button
			OverlayButton exportSetupButton = new OverlayButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.ui.setup.export"), true, (press) -> {
				SetupSharingHandler.exportSetup();
			});
			exportSetupButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.ui.setup.export.btn.desc"), "%n%"));
			setupMenu.addContent(exportSetupButton);

			//Import Menu
			ContextMenu importMenu = new ContextMenu();
			importMenu.setAutoclose(true);
			setupMenu.addChild(importMenu);

			//Import Button
			OverlayButton importSetupButton = new OverlayButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.ui.setup.import"), true, (press) -> {
				importMenu.setParentButton((AdvancedButton) press);
				importMenu.openMenuAt(0, press.y);
			});
			importSetupButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.ui.setup.import.btn.desc"), "%n%"));
			setupMenu.addContent(importSetupButton);

			//Import -> Choose From Saved Setups
			OverlayButton chooseFromSavedButton = new OverlayButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.setupsharing.import.choosefromsaved"), true, (press) -> {
				SetupSharingHandler.importSetupFromSavedSetups();
			});
			chooseFromSavedButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.setupsharing.import.choosefromsaved.btn.desc"), "%n%"));
			importMenu.addContent(chooseFromSavedButton);

			//Import -> Enter Path
			OverlayButton enterPathButton = new OverlayButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.setupsharing.import.choosefrompath"), true, (press) -> {
				SetupSharingHandler.importSetup();
			});
			enterPathButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.setupsharing.import.choosefrompath.btn.desc"), "%n%"));
			importMenu.addContent(enterPathButton);

			//Restore Menu
			ContextMenu restoreMenu = new ContextMenu();
			restoreMenu.setAutoclose(true);
			setupMenu.addChild(restoreMenu);

			//Restore Button
			OverlayButton restoreButton = new OverlayButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.ui.setup.restore"), true, (press) -> {
				restoreMenu.setParentButton((AdvancedButton) press);
				restoreMenu.openMenuAt(0, press.y);
			});
			restoreButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.ui.setup.restore.btn.desc"), "%n%"));
			setupMenu.addContent(restoreButton);

			//Add entries to restore menu
			try {
				File backups = SetupSharingHandler.SETUP_BACKUP_DIR;
				backups.mkdirs();
				boolean hasContent = false;
				File[] backupsSorted = SetupSharingHandler.sortByDateModified(backups.listFiles());
				for (File f : backupsSorted) {
					if (SetupSharingHandler.isValidSetup(f.getPath())) {
						String btnName = "Backup";
						try {
							LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(f.lastModified()), ZoneId.systemDefault());
							DateTimeFormatter dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
							dtf.withZone(ZoneId.systemDefault());
							dtf.withLocale(Locale.getDefault());
							btnName = dt.format(dtf);
						} catch (Exception e3) {
							e3.printStackTrace();
						}
						OverlayButton backupEntryButton = new OverlayButton(0, 0, 0, 0, btnName, true, (press) -> {
							FMYesNoPopup backupConfirmPop = new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
								if (call) {
									new Thread(() -> {
										SetupSharingHandler.StatusPopup restoreBlockerPopup = new SetupSharingHandler.StatusPopup(Locals.localize("fancymenu.helper.setupsharing.restore.restoring"));
										PopupHandler.displayPopup(restoreBlockerPopup);
										try {
											try {
												File fmFolder = FancyMenu.MOD_DIR;
												if (fmFolder.isDirectory()) {
													org.apache.commons.io.FileUtils.deleteDirectory(fmFolder);
												}
											} catch (Exception e2) {
												e2.printStackTrace();
											}
											File homeRaw = Minecraft.getInstance().gameDirectory;
											File home = new File(homeRaw.getAbsolutePath().replace("\\", "/"));
											if (home.isDirectory()) {
												//Check that we're really in the correct dir, because I'm paranoid
												File config = new File(home.getPath() + "/config");
												if (config.isDirectory()) {
													File backupInstance = new File(f.getPath() + "/setup");
													if (backupInstance.isDirectory()) {
														org.apache.commons.io.FileUtils.copyDirectory(backupInstance, home);
														FMNotificationPopup pop = new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, () -> {
															ScreenCustomization.reloadFancyMenu();
														}, Locals.localize("fancymenu.helper.setupsharing.restore.success"));
														PopupHandler.displayPopup(pop);
													}
												}
											}
										} catch (Exception e) {
											e.printStackTrace();
											FMYesNoPopup pop = new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call2) -> {
												if (call2) {
													try {
														ScreenCustomization.openFile(SetupSharingHandler.SETUP_BACKUP_DIR);
													} catch (Exception e3) {
														e3.printStackTrace();
													}
												}
												ScreenCustomization.reloadFancyMenu();
											}, Locals.localize("fancymenu.helper.setupsharing.restore.error"));
											PopupHandler.displayPopup(pop);
										}
										restoreBlockerPopup.setDisplayed(false);
									}).start();
								}
							}, Locals.localize("fancymenu.helper.setupsharing.restore.confirm"));
							PopupHandler.displayPopup(backupConfirmPop);
						});
						backupEntryButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.ui.setup.restore.entry.btn.desc"), "%n%"));
						restoreMenu.addContent(backupEntryButton);
						hasContent = true;
					}
				}
				if (!hasContent) {
					OverlayButton emptyButton = new OverlayButton(0, 0, 0, 0, "----------", true, (press) -> {});
					restoreMenu.addContent(emptyButton);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			/** SETUP TAB END **/

			/** CUSTOM GUI TAB START **/
			ContextMenu customGuiMenu = new ContextMenu();
			customGuiMenu.setAutoclose(true);
			bar.addChild(customGuiMenu, "fm.ui.tab.customguis", ElementAlignment.LEFT);

			OverlayButton newCustomGuiButton = new OverlayButton(0, 0, 0, 0, Locals.localize("helper.ui.customguis.new"), true, (press) -> {
				PopupHandler.displayPopup(new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
					if (call) {
						PopupHandler.displayPopup(new CreateCustomGuiPopup());
					}
				}, Locals.localize("helper.ui.customguis.new.sure")));
			});
			newCustomGuiButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.customguis.new.desc"), "%n%"));
			customGuiMenu.addContent(newCustomGuiButton);

			ManageCustomGuiContextMenu manageCustomGuiMenu = new ManageCustomGuiContextMenu();
			manageCustomGuiMenu.setAutoclose(true);
			customGuiMenu.addChild(manageCustomGuiMenu);

			OverlayButton manageCustomGuiButton = new OverlayButton(0, 0, 0, 0, Locals.localize("helper.ui.customguis.manage"), true, (press) -> {
				manageCustomGuiMenu.setParentButton((AdvancedButton) press);
				manageCustomGuiMenu.openMenuAt(0, press.y);
			});
			manageCustomGuiButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.customguis.manage.desc"), "%n%"));
			customGuiMenu.addContent(manageCustomGuiButton);

			OverlayButton customGuiTab = new OverlayButton(0, 0, 0, 0, Locals.localize("helper.ui.customguis"), true, (press) -> {
				customGuiMenu.setParentButton((AdvancedButton) press);
				customGuiMenu.openMenuAt(press.x, press.y + press.getHeight());
			});
			if (FancyMenu.getConfig().getOrDefault("advancedmode", false)) {
				bar.addElement(customGuiTab, "fm.ui.tab.customguis", ElementAlignment.LEFT, false);
			}
			/** CUSTOM GUI TAB END **/

			/** TOOLS TAB START **/
			ContextMenu toolsMenu = new ContextMenu();
			toolsMenu.setAutoclose(true);
			bar.addChild(toolsMenu, "fm.ui.tab.tools", ElementAlignment.LEFT);

			OverlayButton menuInfoButton = new OverlayButton(0, 0, 0, 0, Locals.localize("helper.ui.tools.menuinfo.off"), true, (press) -> {
				if (showMenuInfo) {
					showMenuInfo = false;
					((AdvancedButton)press).setMessage(Locals.localize("helper.ui.tools.menuinfo.off"));
				} else {
					showMenuInfo = true;
					((AdvancedButton)press).setMessage(Locals.localize("helper.ui.tools.menuinfo.on"));
				}
			});
			if (showMenuInfo) {
				menuInfoButton.setMessage(Locals.localize("helper.ui.tools.menuinfo.on"));
			}
			menuInfoButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.tools.menuinfo.desc"), "%n%"));
			toolsMenu.addContent(menuInfoButton);

			OverlayButton buttonInfoButton = new OverlayButton(0, 0, 0, 0, Locals.localize("helper.ui.tools.buttoninfo.off"), true, (press) -> {
				if (showButtonInfo) {
					showButtonInfo = false;
					((AdvancedButton)press).setMessage(Locals.localize("helper.ui.tools.buttoninfo.off"));
				} else {
					showButtonInfo = true;
					((AdvancedButton)press).setMessage(Locals.localize("helper.ui.tools.buttoninfo.on"));
				}
			}) {
				@Override
				public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
					Screen current = Minecraft.getInstance().screen;
					if ((current != null) && ScreenCustomization.isCustomizationEnabledForScreen(current)) {
						this.active = true;
						this.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.tools.buttoninfo.desc"), "%n%"));
					} else {
						this.active = false;
						this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.ui.tools.buttoninfo.enablecustomizations"), "%n%"));
					}
					super.render(p_93657_, p_93658_, p_93659_, p_93660_);
				}
			};
			if (showButtonInfo) {
				buttonInfoButton.setMessage(Locals.localize("helper.ui.tools.buttoninfo.on"));
			}
			toolsMenu.addContent(buttonInfoButton);

			OverlayButton clearVariablesButton = new OverlayButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.ui.tools.clear_variables"), true, (press) -> {
				FMYesNoPopup p = new FMYesNoPopup(300, new Color(0,0,0,0), 240, (call) -> {
					if (call) {
						VariableHandler.clearVariables();
					}
				}, StringUtils.splitLines(Locals.localize("fancymenu.helper.ui.tools.clear_variables.confirm"), "%n%"));
				PopupHandler.displayPopup(p);
			});
			clearVariablesButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.ui.tools.clear_variables.desc"), "%n%"));
			toolsMenu.addContent(clearVariablesButton);

			OverlayButton toolsTab = new OverlayButton(0, 0, 0, 0, Locals.localize("helper.ui.tools"), true, (press) -> {
				toolsMenu.setParentButton((AdvancedButton) press);
				toolsMenu.openMenuAt(press.x, press.y + press.getHeight());
			});
			bar.addElement(toolsTab, "fm.ui.tab.tools", ElementAlignment.LEFT, false);
			/** TOOLS TAB END **/

			/** MISCELLANEOUS TAB START **/
			ContextMenu miscMenu = new ContextMenu();
			miscMenu.setAutoclose(true);
			bar.addChild(miscMenu, "fm.ui.tab.misc", ElementAlignment.LEFT);

			OverlayButton closeGuiButton = new OverlayButton(0, 0, 0, 0, Locals.localize("helper.ui.misc.closegui"), true, (press) -> {
				Minecraft.getInstance().setScreen(null);
			});
			closeGuiButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.misc.closegui.desc"), "%n%"));
			miscMenu.addContent(closeGuiButton);

			OverlayButton openWorldLoadingScreenButton = new OverlayButton(0, 0, 0, 0, Locals.localize("helper.ui.misc.openworldloading"), true, (press) -> {
				LevelLoadingScreen wl = new LevelLoadingScreen(new StoringChunkProgressListener(0));
				Minecraft.getInstance().setScreen(wl);
			});
			openWorldLoadingScreenButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.misc.openworldloading.desc"), "%n%"));
			miscMenu.addContent(openWorldLoadingScreenButton);

			OverlayButton openMessageScreenButton = new OverlayButton(0, 0, 0, 0, Locals.localize("helper.ui.misc.openmessagescreen"), true, (press) -> {
				Minecraft.getInstance().setScreen(new GenericDirtMessageScreen(Component.literal("hello ・ω・")));
			});
			openMessageScreenButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.misc.openmessagescreen.desc"), "%n%"));
			miscMenu.addContent(openMessageScreenButton);

			OverlayButton openProgressScreenButton = new OverlayButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.ui.misc.open_progress_screen"), true, (press) -> {
				ProgressScreen s = new ProgressScreen(false);
				s.progressStage(Component.literal("dummy stage name"));
				s.progressStagePercentage(50);
				Minecraft.getInstance().setScreen(s);
			});
			openProgressScreenButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.ui.misc.open_progress_screen.btn.desc"), "%n%"));
			miscMenu.addContent(openProgressScreenButton);

			OverlayButton openReceivingLevelScreenButton = new OverlayButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.ui.misc.receiving_level_screen"), true, (press) -> {
				ReceivingLevelScreen s = new ReceivingLevelScreen();
				Minecraft.getInstance().setScreen(s);
			}) {
				@Override
				public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
					if (Minecraft.getInstance().level == null) {
						this.active = true;
					} else {
						this.active = false;
					}
					super.render(p_93657_, p_93658_, p_93659_, p_93660_);
				}
			};
			openReceivingLevelScreenButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.ui.misc.receiving_level_screen.btn.desc"), "%n%"));
			miscMenu.addContent(openReceivingLevelScreenButton);

			OverlayButton openConnectScreenButton = new OverlayButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.ui.misc.open_connect_screen"), true, (press) -> {
				ConnectScreen.startConnecting(new TitleScreen(), Minecraft.getInstance(), new ServerAddress("%fancymenu_dummy_address%", 25565), null);
			}) {
				@Override
				public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
					if (Minecraft.getInstance().level == null) {
						this.active = true;
					} else {
						this.active = false;
					}
					super.render(p_93657_, p_93658_, p_93659_, p_93660_);
				}
			};
			openConnectScreenButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.ui.misc.open_connect_screen.btn.desc"), "%n%"));
			miscMenu.addContent(openConnectScreenButton);

			OverlayButton miscTab = new OverlayButton(0, 0, 0, 0, Locals.localize("helper.ui.misc"), true, (press) -> {
				miscMenu.setParentButton((AdvancedButton) press);
				miscMenu.openMenuAt(press.x, press.y + press.getHeight());
			});
			bar.addElement(miscTab, "fm.ui.tab.misc", ElementAlignment.LEFT, false);
			/** MISCELLANEOUS TAB END **/

			/** CLOSE GUI BUTTON START **/
			AdvancedImageButton closeGuiButtonTab = new AdvancedImageButton(20, 20, 20, 20, CLOSE_BUTTON_TEXTURE, true, (press) -> {
				Minecraft.getInstance().setScreen(null);
			}) {
				@Override
				public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
					this.width = this.height;
					super.render(matrix, mouseX, mouseY, partialTicks);
				}
			};
			closeGuiButtonTab.ignoreLeftMouseDownClickBlock = true;
			closeGuiButtonTab.ignoreBlockedInput = true;
			closeGuiButtonTab.enableRightclick = true;
			closeGuiButtonTab.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.misc.closegui.desc"), "%n%"));
			bar.addElement(closeGuiButtonTab, "fm.ui.tab.closegui", ElementAlignment.RIGHT, false);
			/** CLOSE GUI BUTTON END **/

			/** RELOAD BUTTON START **/
			AdvancedImageButton reloadButtonTab = new AdvancedImageButton(20, 20, 20, 20, RELOAD_BUTTON_TEXTURE, true, (press) -> {
				ScreenCustomization.reloadFancyMenu();
			}) {
				@Override
				public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
					this.width = this.height;
					super.render(matrix, mouseX, mouseY, partialTicks);
				}
			};
			reloadButtonTab.ignoreLeftMouseDownClickBlock = true;
			reloadButtonTab.ignoreBlockedInput = true;
			reloadButtonTab.enableRightclick = true;
			reloadButtonTab.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.reload.desc"), "%n%"));
			bar.addElement(reloadButtonTab, "fm.ui.tab.reload", ElementAlignment.RIGHT, false);
			/** RELOAD BUTTON END **/

			AdvancedButton expandButton = bar.getElement("menubar.default.extendbtn");
			if (expandButton != null) {
				if (expandButton instanceof AdvancedImageButton) {
					if (!extended) {
						((AdvancedImageButton)expandButton).setImage(MenuBar.EXPAND_BTN_TEXTURE);
						
						expandButton.setDescription(StringUtils.splitLines(Locals.localize("helper.menubar.expand"), "%n%"));
					}
				}
			}

			bar.setExtended(extended);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void render(PoseStack matrix, Screen screen) {
		try {

			if (bar != null) {
				if (!PopupHandler.isPopupActive()) {
					if (FancyMenu.getConfig().getOrDefault("showcustomizationbuttons", true)) {
						if (!(screen instanceof LayoutEditorScreen) && !(screen instanceof ConfigScreen) && !(screen instanceof GameIntroScreen) && AnimationHandler.isReady() && ScreenCustomization.isValidScreen(screen)) {

							RenderUtils.setZLevelPre(matrix, 400);

							renderMenuInfo(matrix, screen);

							renderUnicodeWarning(matrix, screen);

							renderButtonInfo(matrix, screen);

							renderButtonInfoWarning(matrix, screen);

							RenderUtils.setZLevelPost(matrix);

							bar.render(matrix, screen);

						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static void renderButtonInfo(PoseStack matrix, Screen screen) {
		if (showButtonInfo) {
			boolean isButtonHovered = false;
			for (ButtonData d : buttons) {
				if (d.getButton().isHoveredOrFocused()) {
					isButtonHovered = true;
					long id = d.getId();
					String idString = Locals.localize("helper.buttoninfo.idnotfound");
					if (id >= 0) {
						idString = String.valueOf(id);
					}
					if (d.getCompatibilityId() != null) {
						idString = d.getCompatibilityId();
					}
					List<String> info = new ArrayList<>();
					int width = Minecraft.getInstance().font.width(Locals.localize("helper.button.buttoninfo")) + 10;
					long now = System.currentTimeMillis();
					info.add("§f" + Locals.localize("helper.buttoninfo.id") + ": " + idString);
					info.add("§f" + Locals.localize("general.width") + ": " + d.getButton().getWidth());
					info.add("§f" + Locals.localize("general.height") + ": " + d.getButton().getHeight());
					info.add("§f" + Locals.localize("helper.buttoninfo.labelwidth") + ": " + Minecraft.getInstance().font.width(d.getButton().getMessage().getString()));
					info.add("");
					if (lastButtonInfoRightClick + 2000 < now) {
						info.add(Locals.localize("fancymenu.helper.button_info.copy_locator"));
					} else {
						info.add(Locals.localize("fancymenu.helper.button_info.copy_locator.copied"));
					}
					if (MouseInput.isRightMouseDown()) {
						Screen current = Minecraft.getInstance().screen;
						String locator = current.getClass().getName() + ":" + idString;
						Minecraft.getInstance().keyboardHandler.setClipboard(locator);
						lastButtonInfoRightClick = now;
					}
					for (String s : info) {
						int i = Minecraft.getInstance().font.width(s) + 10;
						if (i > width) {
							width = i;
						}
					}
					matrix.pushPose();
					matrix.scale(getUIScale(), getUIScale(), getUIScale());
					MouseInput.setRenderScale(getUIScale());
					int x = MouseInput.getMouseX();
					if ((screen.width / getUIScale()) < x + width + 10) {
						x -= width + 10;
					}
					int y = MouseInput.getMouseY();
					if ((screen.height / getUIScale()) < y + 80) {
						y -= 90;
					}
					fill(matrix, x, y, x + width + 10, y + 100, BUTTON_INFO_BACKGROUND_COLOR.getRGB());
					RenderSystem.enableBlend();
					drawString(matrix, Minecraft.getInstance().font, "§f§l" + Locals.localize("helper.button.buttoninfo"), x + 10, y + 10, -1);
					int i2 = 20;
					for (String s : info) {
						drawString(matrix, Minecraft.getInstance().font, s, x + 10, y + 10 + i2, -1);
						i2 += 10;
					}
					MouseInput.resetRenderScale();
					matrix.popPose();
					RenderSystem.disableBlend();
					break;
				}
			}
			if (!isButtonHovered) {
				lastButtonInfoRightClick = 0;
			}
		}
	}

	protected static void renderButtonInfoWarning(PoseStack matrix, Screen screen) {
		if (showButtonInfo && !ScreenCustomization.isCustomizationEnabledForScreen(screen)) {
			List<String> info = new ArrayList<>();
			int width = Minecraft.getInstance().font.width(Locals.localize("fancymenu.helper.ui.tools.buttoninfo.enablecustomizations.cursorwarning.line1")) + 10;

			info.add(Locals.localize("fancymenu.helper.ui.tools.buttoninfo.enablecustomizations.cursorwarning.line2"));
			info.add(Locals.localize("fancymenu.helper.ui.tools.buttoninfo.enablecustomizations.cursorwarning.line3"));

			for (String s : info) {
				int i = Minecraft.getInstance().font.width(s) + 10;
				if (i > width) {
					width = i;
				}
			}

			matrix.pushPose();

			matrix.scale(getUIScale(), getUIScale(), getUIScale());

			MouseInput.setRenderScale(getUIScale());

			int x = MouseInput.getMouseX();
			if ((screen.width / getUIScale()) < x + width + 10) {
				x -= width + 10;
			}

			int y = MouseInput.getMouseY();
			if ((screen.height / getUIScale()) < y + 80) {
				y -= 90;
			}

			fill(matrix, x, y, x + width + 10, y + 60, WARNING_COLOR.getRGB());

			RenderSystem.enableBlend();
			drawString(matrix, Minecraft.getInstance().font, "§f§l" + Locals.localize("fancymenu.helper.ui.tools.buttoninfo.enablecustomizations.cursorwarning.line1"), x + 10, y + 10, -1);

			int i2 = 20;
			for (String s : info) {
				drawString(matrix, Minecraft.getInstance().font, s, x + 10, y + 10 + i2, -1);
				i2 += 10;
			}

			MouseInput.resetRenderScale();

			matrix.popPose();

			RenderSystem.disableBlend();
		}
	}

	protected static void renderMenuInfo(PoseStack matrix, Screen screen) {
		if (showMenuInfo) {
			String infoTitle = "§f§l" + Locals.localize("helper.menuinfo.identifier") + ":";
			String id = "";
			if (screen instanceof CustomGuiBase) {
				id = ((CustomGuiBase)screen).getIdentifier();
			} else {
				id = screen.getClass().getName();
			}
			int w = Minecraft.getInstance().font.width(infoTitle);
			int w2 = Minecraft.getInstance().font.width(id);
			if (w2 > w) {
				w = w2;
			}
			int h = bar.getHeight() + 5;

			RenderSystem.enableBlend();

			matrix.pushPose();

			matrix.scale(getUIScale(), getUIScale(), getUIScale());

			fill(matrix, 3, h, 3 + w + 4, h + 23, MENU_INFO_BACKGROUND_COLOR.getRGB());

			drawString(matrix, Minecraft.getInstance().font, infoTitle, 5, h + 2, 0);
			if (tick == 0) {
				drawString(matrix, Minecraft.getInstance().font, "§f" + id, 5, h + 13, 0);
			} else {
				drawString(matrix, Minecraft.getInstance().font, "§a" + Locals.localize("helper.menuinfo.idcopied"), 5, h + 13, 0);
			}

			MouseInput.setRenderScale(getUIScale());

			int mouseX = MouseInput.getMouseX();
			int mouseY = MouseInput.getMouseY();
			if (!bar.isChildOpen()) {
				if ((mouseX >= 5) && (mouseX <= 5 + w2) && (mouseY >= h + 13) && (mouseY <= h + 13 + 10) && (tick == 0)) {
					fill(matrix, 5, h + 13 + 10 - 1, 5 + w2, h + 13 + 10, -1);

					if (MouseInput.isLeftMouseDown()) {
						tick++;
						Minecraft.getInstance().keyboardHandler.setClipboard(id);
					}
				}
			}
			if (tick > 0) {
				if (tick < 60) {
					tick++;
				} else {
					tick = 0;
				}
			}

			MouseInput.resetRenderScale();

			matrix.popPose();

			RenderSystem.disableBlend();
		}
	}

	protected static void renderUnicodeWarning(PoseStack matrix, Screen screen) {
		if (!FancyMenu.getConfig().getOrDefault("show_unicode_warning", true)) {
			return;
		}
		if (Minecraft.getInstance().options.forceUnicodeFont().get()) {
			String title = Locals.localize("helper.ui.warning");
			int w = Minecraft.getInstance().font.width(title);
			String[] lines = StringUtils.splitLines(Locals.localize("helper.ui.warning.unicode"), "%n%");
			for (String s : lines) {
				int w2 = Minecraft.getInstance().font.width(s);
				if (w2 > w) {
					w = w2;
				}
			}

			int x = screen.width - w - 5;
			int y = (int) ((bar.getHeight() + 5) * UIBase.getUIScale());

			RenderSystem.enableBlend();

			int h = 13;
			if (lines.length > 0) {
				h += 10*lines.length;
			}
			fill(matrix, x - 4, y, x + w + 2, y + h, WARNING_COLOR.getRGB());

			drawString(matrix, Minecraft.getInstance().font, title, x, y + 2, Color.WHITE.getRGB());

			int i = 0;
			for (String s : lines) {
				drawString(matrix, Minecraft.getInstance().font, s, x, y + 13 + i, Color.WHITE.getRGB());
				i += 10;
			}

			RenderSystem.disableBlend();
		}
	}

	@EventListener
	public void onButtonsCached(ButtonCacheUpdatedEvent e) {
		buttons = e.getButtonDataList();
	}

	@EventListener
	public void onInitScreen(InitOrResizeScreenEvent.Pre e) {
		try {

			if (e.getScreen() != null) {
				if (FancyMenu.getConfig().getOrDefault("showcustomizationbuttons", true)) {

					updateUI();

				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static class ManageCustomGuiContextMenu extends ContextMenu {

		private ManageCustomGuiSubContextMenu manageMenu;

		public ManageCustomGuiContextMenu() {

			this.manageMenu = new ManageCustomGuiSubContextMenu();
			this.addChild(this.manageMenu);

		}

		@Override
		public void openMenuAt(int x, int y) {

			this.content.clear();

			List<String> l = CustomGuiLoader.getCustomGuis();
			if (!l.isEmpty()) {

				this.addContent(new OverlayButton(0, 0, 0, 0, Locals.localize("helper.buttons.tools.customguis.openbyname"), true, (press) -> {
					PopupHandler.displayPopup(new FMTextInputPopup(new Color(0, 0, 0, 0), Locals.localize("helper.buttons.tools.customguis.openbyname"), null, 240, (call) -> {
						if (call != null) {
							if (CustomGuiLoader.guiExists(call)) {
								Minecraft.getInstance().setScreen(CustomGuiLoader.getGui(call, Minecraft.getInstance().screen, null));
							} else {
								PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("helper.buttons.tools.customguis.invalididentifier")));
							}
						}
					}));
				}));

				this.addContent(new OverlayButton(0, 0, 0, 0, Locals.localize("helper.buttons.tools.customguis.deletebyname"), true, (press) -> {
					PopupHandler.displayPopup(new FMTextInputPopup(new Color(0, 0, 0, 0), Locals.localize("helper.buttons.tools.customguis.deletebyname"), null, 240, (call) -> {
						if (call != null) {
							if (CustomGuiLoader.guiExists(call)) {
								PopupHandler.displayPopup(new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call2) -> {
									if (call2) {
										if (CustomGuiLoader.guiExists(call)) {
											List<File> delete = new ArrayList<File>();
											for (String s : FileUtils.getFiles(FancyMenu.getCustomGuisDirectory().getPath())) {
												File f = new File(s);
												for (String s2 : FileUtils.getFileLines(f)) {
													if (s2.replace(" ", "").toLowerCase().equals("identifier=" + call)) {
														delete.add(f);
													}
												}
											}

											for (File f : delete) {
												if (f.isFile()) {
													f.delete();
												}
											}

											ScreenCustomization.reloadFancyMenu();
										}
									}
								}, Locals.localize("helper.buttons.tools.customguis.sure")));
							} else {
								PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("helper.buttons.tools.customguis.invalididentifier")));
							}
						}
					}));
				}));

				this.addSeparator();

				for (String s : l) {
					String label = s;
					if (Minecraft.getInstance().font.width(label) > 80) {
						label = Minecraft.getInstance().font.plainSubstrByWidth(label, 75) + "..";
					}

					this.addContent(new OverlayButton(0, 0, 0, 0, label, true, (press) -> {
						this.manageMenu.setParentButton((AdvancedButton) press);
						this.manageMenu.openMenuAt(0, press.y, s);
					}));
				}

			}

			super.openMenuAt(x, y);

		}

	}

	private static class ManageCustomGuiSubContextMenu extends ContextMenu {

		public void openMenuAt(int x, int y, String customGuiIdentifier) {
			this.content.clear();

			OverlayButton openMenuButton = new OverlayButton(0, 0, 0, 0, Locals.localize("helper.buttons.tools.customguis.open"), (press) -> {
				if (CustomGuiLoader.guiExists(customGuiIdentifier)) {
					Minecraft.getInstance().setScreen(CustomGuiLoader.getGui(customGuiIdentifier, Minecraft.getInstance().screen, null));
				}
			});
			this.addContent(openMenuButton);

			OverlayButton deleteMenuButton = new OverlayButton(0, 0, 0, 0, Locals.localize("helper.buttons.tools.customguis.delete"), (press) -> {
				PopupHandler.displayPopup(new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
					if (call) {
						if (CustomGuiLoader.guiExists(customGuiIdentifier)) {
							List<File> delete = new ArrayList<File>();
							for (String s : FileUtils.getFiles(FancyMenu.getCustomGuisDirectory().getPath())) {
								File f = new File(s);
								for (String s2 : FileUtils.getFileLines(f)) {
									if (s2.replace(" ", "").toLowerCase().equals("identifier=" + customGuiIdentifier)) {
										delete.add(f);
									}
								}
							}

							for (File f : delete) {
								if (f.isFile()) {
									f.delete();
								}
							}

							ScreenCustomization.reloadFancyMenu();
						}
					}
				}, Locals.localize("helper.buttons.tools.customguis.sure")));
			});
			this.addContent(deleteMenuButton);

			this.openMenuAt(x, y);
		}

	}

	private static class ManageLayoutsContextMenu extends ContextMenu {

		private ManageLayoutsSubContextMenu manageSubPopup;
		private boolean isUniversal;

		public ManageLayoutsContextMenu(boolean isUniversal) {

			this.isUniversal = isUniversal;

			this.manageSubPopup = new ManageLayoutsSubContextMenu();
			this.addChild(this.manageSubPopup);

		}

		public void openMenuAt(AbstractWidget parentBtn) {
			this.content.clear();

			String identifier = Minecraft.getInstance().screen.getClass().getName();
			if (Minecraft.getInstance().screen instanceof CustomGuiBase) {
				identifier = ((CustomGuiBase) Minecraft.getInstance().screen).getIdentifier();
			}
			if (this.isUniversal) {
				identifier = "%fancymenu:universal_layout%";
			}

			List<PropertiesSet> enabled = LayoutHandler.getEnabledLayoutsForMenuIdentifier(identifier);
			if (!this.isUniversal) {
				List<PropertiesSet> sets = new ArrayList<>();
				for (PropertiesSet s : enabled) {
					List<PropertiesSection> metas = s.getPropertiesOfType("customization-meta");
					if (!metas.isEmpty()) {
						PropertiesSection meta = metas.get(0);
						String id = meta.getEntryValue("identifier");
						if (!id.equals("%fancymenu:universal_layout%")) {
							sets.add(s);
						}
					}
				}
				enabled = sets;
			}
			if (!enabled.isEmpty()) {
				for (PropertiesSet s : enabled) {
					List<PropertiesSection> secs = s.getPropertiesOfType("customization-meta");
					if (secs.isEmpty()) {
						secs = s.getPropertiesOfType("type-meta");
					}
					if (!secs.isEmpty()) {
						String name = "<missing name>";
						PropertiesSection meta = secs.get(0);
						File f = new File(meta.getEntryValue("path"));
						if (f.isFile()) {
							name = Files.getNameWithoutExtension(f.getName());

							int totalactions = s.getProperties().size() - 1;
							OverlayButton layoutEntryBtn = new OverlayButton(0, 0, 0, 0, "§a" + name, (press) -> {
								this.manageSubPopup.setParentButton((AdvancedButton) press);
								this.manageSubPopup.openMenuAt(0, press.y, f, false);
							});
							layoutEntryBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.customization.managelayouts.layout.btndesc", Locals.localize("helper.buttons.customization.managelayouts.enabled"), "" + totalactions), "%n%"));
							this.addContent(layoutEntryBtn);
						}
					}
				}
			}

			List<PropertiesSet> disabled = LayoutHandler.getDisabledLayoutsForMenuIdentifier(identifier);
			if (!this.isUniversal) {
				List<PropertiesSet> sets = new ArrayList<>();
				for (PropertiesSet s : disabled) {
					List<PropertiesSection> metas = s.getPropertiesOfType("customization-meta");
					if (!metas.isEmpty()) {
						PropertiesSection meta = metas.get(0);
						String id = meta.getEntryValue("identifier");
						if (!id.equals("%fancymenu:universal_layout%")) {
							sets.add(s);
						}
					}
				}
				disabled = sets;
			}
			if (!disabled.isEmpty()) {
				for (PropertiesSet s : disabled) {
					List<PropertiesSection> secs = s.getPropertiesOfType("customization-meta");
					if (secs.isEmpty()) {
						secs = s.getPropertiesOfType("type-meta");
					}
					if (!secs.isEmpty()) {
						String name = "<missing name>";
						PropertiesSection meta = secs.get(0);
						File f = new File(meta.getEntryValue("path"));
						if (f.isFile()) {
							name = Files.getNameWithoutExtension(f.getName());

							int totalactions = s.getProperties().size() - 1;
							OverlayButton layoutEntryBtn = new OverlayButton(0, 0, 0, 0, "§c" + name, (press) -> {
								this.manageSubPopup.setParentButton((AdvancedButton) press);
								this.manageSubPopup.openMenuAt(0, press.y, f, true);
							});
							layoutEntryBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.customization.managelayouts.layout.btndesc", Locals.localize("helper.buttons.customization.managelayouts.disabled"), "" + totalactions), "%n%"));
							this.addContent(layoutEntryBtn);
						}
					}
				}
			}

			if (enabled.isEmpty() && disabled.isEmpty()) {
				OverlayButton emptyBtn = new OverlayButton(0, 0, 0, 0, Locals.localize("helper.creator.empty"), (press) -> {});
				this.addContent(emptyBtn);
			}

			this.openMenuAt(parentBtn.x - this.getWidth() - 2, parentBtn.y);
		}

		@Override
		public void render(PoseStack matrix, int mouseX, int mouseY) {
			super.render(matrix, mouseX, mouseY);

			if (this.manageSubPopup != null) {
				this.manageSubPopup.render(matrix, mouseX, mouseY);
				if (!this.isOpen()) {
					this.manageSubPopup.closeMenu();
				}
			}
		}

		@Override
		public void closeMenu() {
			if (!this.manageSubPopup.isHovered()) {
				super.closeMenu();
			}
		}

		@Override
		public boolean isHovered() {
			if (this.manageSubPopup.isOpen() && this.manageSubPopup.isHovered()) {
				return true;
			} else {
				return super.isHovered();
			}
		}

	}

	private static class ManageLayoutsSubContextMenu extends ContextMenu {

		public void openMenuAt(int x, int y, File layout, boolean disabled) {

			this.content.clear();

			String toggleLabel = Locals.localize("helper.buttons.customization.managelayouts.disable");
			if (disabled) {
				toggleLabel = Locals.localize("helper.buttons.customization.managelayouts.enable");
			}
			OverlayButton toggleLayoutBtn = new OverlayButton(0, 0, 0, 0, toggleLabel, (press) -> {
				if (disabled) {
					String name = FileUtils.generateAvailableFilename(FancyMenu.getCustomizationsDirectory().getPath(), Files.getNameWithoutExtension(layout.getName()), "txt");
					FileUtils.copyFile(layout, new File(FancyMenu.getCustomizationsDirectory().getPath() + "/" + name));
					layout.delete();
				} else {
					String disPath = FancyMenu.getCustomizationsDirectory().getPath() + "/.disabled";
					String name = FileUtils.generateAvailableFilename(disPath, Files.getNameWithoutExtension(layout.getName()), "txt");
					FileUtils.copyFile(layout, new File(disPath + "/" + name));
					layout.delete();
				}
				ScreenCustomization.reloadFancyMenu();
			});
			this.addContent(toggleLayoutBtn);

			OverlayButton editLayoutBtn = new OverlayButton(0, 0, 0, 0, Locals.localize("helper.ui.current.layouts.manage.edit"), (press) -> {
				Screen s = Minecraft.getInstance().screen;
				if ((this.parent != null) && (this.parent instanceof ManageLayoutsContextMenu)) {
					if (((ManageLayoutsContextMenu)this.parent).isUniversal) {
						s = new CustomGuiBase("", "%fancymenu:universal_layout%", true, Minecraft.getInstance().screen, null);
					}
				}
				LayoutHandler.editLayout(s, layout);
			});
			editLayoutBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.current.layouts.manage.edit.desc"), "%n%"));
			this.addContent(editLayoutBtn);

			OverlayButton openInTextEditorBtn = new OverlayButton(0, 0, 0, 0, Locals.localize("helper.buttons.customization.managelayouts.openintexteditor"), (press) -> {
				ScreenCustomization.openFile(layout);
			});
			openInTextEditorBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.customization.managelayouts.openintexteditor.desc"), "%n%"));
			this.addContent(openInTextEditorBtn);

			OverlayButton deleteLayoutBtn = new OverlayButton(0, 0, 0, 0, Locals.localize("helper.buttons.customization.managelayouts.delete"), (press) -> {
				PopupHandler.displayPopup(new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
					if (call) {
						if (layout.exists()) {
							layout.delete();
							ScreenCustomization.reloadFancyMenu();
						}
					}
				}, Locals.localize("helper.buttons.customization.managelayouts.delete.msg"), "", "", "", ""));
				ScreenCustomization.reloadFancyMenu();
			});
			this.addContent(deleteLayoutBtn);

			this.openMenuAt(x, y);

		}
	}

	private static class OverrideMenuContextMenu extends ContextMenu {

		@Override
		public void openMenuAt(int x, int y) {

			this.content.clear();

			List<String> l = CustomGuiLoader.getCustomGuis();

			if (!l.isEmpty()) {

				this.addContent(new OverlayButton(0, 0, 0, 0, Locals.localize("helper.buttons.tools.customguis.pickbyname"), true, (press) -> {
					PopupHandler.displayPopup(new TextInputPopup(new Color(0, 0, 0, 0), Locals.localize("helper.buttons.tools.customguis.pickbyname"), null, 240, (call) -> {
						if (call != null) {
							if (CustomGuiLoader.guiExists(call)) {
								onOverrideWithCustomGui(Minecraft.getInstance().screen, call);
							} else {
								PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("helper.buttons.tools.customguis.invalididentifier")));
							}
						}
					}));
				}));

				this.addSeparator();

				for (String s : l) {
					String label = s;
					if (Minecraft.getInstance().font.width(label) > 80) {
						label = Minecraft.getInstance().font.plainSubstrByWidth(label, 75) + "..";
					}

					this.addContent(new OverlayButton(0, 0, 0, 0, label, true, (press) -> {
						onOverrideWithCustomGui(Minecraft.getInstance().screen, s);
					}));

				}

			} else {
				this.addContent(new OverlayButton(0, 0, 0, 0, Locals.localize("helper.creator.empty"), true, (press) -> {}));
			}

			super.openMenuAt(x, y);

		}

	}

	private static void onOverrideWithCustomGui(Screen current, String customGuiIdentifier) {
		if ((customGuiIdentifier != null) && CustomGuiLoader.guiExists(customGuiIdentifier)) {
			PropertiesSection meta = new PropertiesSection("customization-meta");
			meta.addEntry("identifier", current.getClass().getName());

			PropertiesSection or = new PropertiesSection("customization");
			or.addEntry("action", "overridemenu");
			or.addEntry("identifier", customGuiIdentifier);

			PropertiesSet props = new PropertiesSet("menu");
			props.addProperties(meta);
			props.addProperties(or);

			String screenname = current.getClass().getName();
			if (screenname.contains(".")) {
				screenname = new StringBuilder(new StringBuilder(screenname).reverse().toString().split("[.]", 2)[0]).reverse().toString();
			}
			String filename = FileUtils.generateAvailableFilename(FancyMenu.getCustomizationsDirectory().getPath(), "overridemenu_" + screenname, "txt");

			String finalpath = FancyMenu.getCustomizationsDirectory().getPath() + "/" + filename;
			PropertiesSerializer.writeProperties(props, finalpath);

			ScreenCustomization.reloadFancyMenu();
		}
	}

}
