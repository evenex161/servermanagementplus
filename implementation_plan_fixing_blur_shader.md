# Goal Description

Fix the broken backdrop blur shader in the 1.20.1 branch of Servermanagement-Forge. The current implementation uses `GameRenderer.loadEffect`, which applies the blur at the very end of the frame, incorrectly blurring the GUI itself. Additionally, the fragment shader has a bug where alpha values accumulate over 1.0, causing visual artifacts. The goal is to bring the 1.20.1 blur functionality to the same quality level as 1.21.1.

## User Review Required

- **PostChain Instantiation**: The plan replaces the reflection-based `GameRenderer.loadEffect` with a manually managed `PostChain` in `BlurBackdrop.java`.
- **Render Order**: The blur will now be explicitly processed at the beginning of `ScalableContainerScreen.render` (before the background is drawn), ensuring the GUI is drawn cleanly *over* the blurred level.

## Open Questions

None. The issues in the profiling and logs provided indicate a render-order and shader-math bug, which this plan directly addresses.

## Proposed Changes

### Fabric & Forge Shared Shaders

#### [MODIFY] [blur.fsh](file:///C:/VS%20Workspace/Projects/Minecraft%20Mods/Servermanagement-Forge/common/src/main/resources/assets/servermanagement/shaders/program/blur.fsh)
- Fix the alpha accumulation bug. Combine RGB and Alpha into a single `blurred` variable, increment `totalSamples`, and divide the final `fragColor` by `totalSamples` to prevent alpha values from exceeding 1.0.

### Fabric Component

#### [MODIFY] [BlurBackdrop.java](file:///C:/VS%20Workspace/Projects/Minecraft%20Mods/Servermanagement-Forge/fabric/src/main/java/com/servermanagement/gui/BlurBackdrop.java)
- Remove the reflection code targeting `loadEffect` and `shutdownEffect`.
- Introduce a static `net.minecraft.client.renderer.PostChain blurChain` and track the `lastWidth` and `lastHeight` to handle window resizing.
- In `enable()`, instantiate the `PostChain` directly using `Minecraft.getInstance().getTextureManager()`, `getResourceManager()`, and `getMainRenderTarget()`.
- Create a `processBlur(float partialTick)` method that calls `blurChain.resize(...)` if window dimensions changed, invokes `blurChain.process(partialTick)`, and restores the main render target (`mc.getMainRenderTarget().bindWrite(false)`).
- In `disable()`, close the `blurChain` when `activeCount` reaches 0.

#### [MODIFY] [ScalableContainerScreen.java](file:///C:/VS%20Workspace/Projects/Minecraft%20Mods/Servermanagement-Forge/fabric/src/main/java/com/servermanagement/gui/ScalableContainerScreen.java)
- In `render(GuiGraphics g, int mouseX, int mouseY, float partialTick)`, right before `this.renderBackground(g);`, add a call to `BlurBackdrop.processBlur(partialTick)` if `blurEnabled` is true. This ensures the blur is applied to the level exactly before the GUI is drawn over it.

### Forge Component

#### [MODIFY] [BlurBackdrop.java](file:///C:/VS%20Workspace/Projects/Minecraft%20Mods/Servermanagement-Forge/forge/src/main/java/com/servermanagement/gui/BlurBackdrop.java)
- Apply the exact same `PostChain` management changes as the Fabric version.

#### [MODIFY] [ScalableContainerScreen.java](file:///C:/VS%20Workspace/Projects/Minecraft%20Mods/Servermanagement-Forge/forge/src/main/java/com/servermanagement/gui/ScalableContainerScreen.java)
- Apply the exact same `processBlur` call injection as the Fabric version.

## Verification Plan

### Manual Verification
- Compile and run the client in the 1.20.1 environment.
- Open a GUI that extends `ScalableContainerScreen`.
- Verify that the world behind the GUI is blurred, but the GUI itself remains sharp and readable.
- Verify no visual artifacts (like overly bright or completely black screens) occur, confirming the alpha bug is resolved.
- Resize the window while the GUI is open and verify the blur target resizes correctly without crashing.
