/*
 * Copyright © 2020 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of SpruceUI.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package me.lambdaurora.spruceui.widget.container.tabbed;

import me.lambdaurora.spruceui.Position;
import me.lambdaurora.spruceui.background.Background;
import me.lambdaurora.spruceui.background.EmptyBackground;
import me.lambdaurora.spruceui.navigation.NavigationDirection;
import me.lambdaurora.spruceui.widget.AbstractSpruceWidget;
import me.lambdaurora.spruceui.widget.SpruceSeparatorWidget;
import me.lambdaurora.spruceui.widget.SpruceWidget;
import me.lambdaurora.spruceui.widget.WithBackground;
import me.lambdaurora.spruceui.widget.container.AbstractSpruceParentWidget;
import me.lambdaurora.spruceui.widget.container.SpruceContainerWidget;
import me.lambdaurora.spruceui.widget.container.SpruceEntryListWidget;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a container widget with tabs.
 *
 * @author LambdAurora
 * @version 1.7.0
 * @since 1.7.0
 */
public class SpruceTabbedWidget extends AbstractSpruceParentWidget<SpruceWidget> {
    private final Text title;
    private final SideTabList list;
    private final Position anchor;
    private boolean isLeft = false;

    public SpruceTabbedWidget(@NotNull Position position, int width, int height, @Nullable Text title) {
        this(position, width, height, title, Math.max(100, width / 8), title != null ? 20 : 0);
    }

    public SpruceTabbedWidget(@NotNull Position position, int width, int height, @Nullable Text title, int sideWidth, int sideTopOffset) {
        super(position, SpruceWidget.class);
        this.width = width;
        this.height = height;
        this.title = title;
        this.list = new SideTabList(Position.of(position, 0, sideTopOffset), sideWidth, height - sideTopOffset);
        this.anchor = Position.of(this, this.list.getWidth(), 0);
    }

    /**
     * Returns the side tab list.
     *
     * @return the side tab list widget
     */
    public SideTabList getList() {
        return this.list;
    }

    public void addTabEntry(Text title, @Nullable Text description, ContainerFactory factory) {
        this.addTabEntry(title, description, factory.build(this.getWidth() - this.list.getWidth(), this.getHeight()));
    }

    public void addTabEntry(Text title, @Nullable Text description, AbstractSpruceWidget container) {
        TabEntry entry = this.list.addTabEntry(title, description, container);
        entry.container.getPosition().setAnchor(this.anchor);
    }

    public void addSeparatorEntry(Text title) {
        this.list.addSeparatorEntry(title);
    }

    @Override
    public void setFocused(@Nullable Element focused) {
        super.setFocused(focused);
    }

    @Override
    public List<SpruceWidget> children() {
        if (this.list.getCurrentTab() == null)
            return Collections.singletonList(this.list);
        return Arrays.asList(this.list, this.list.getCurrentTab().container);
    }

    /* Navigation */

    @Override
    public boolean onNavigation(@NotNull NavigationDirection direction, boolean tab) {
        if (this.requiresCursor()) return false;

        if (tab) {
            boolean result = this.list.getCurrentTab().container.onNavigation(direction, tab);
            this.setFocused(this.list.getCurrentTab().container.isFocused() ? this.list.getCurrentTab().container : null);
            return result;
        }

        if (direction.isHorizontal()) {
            if (direction == NavigationDirection.RIGHT) {
                if (this.list.getCurrentTab().container.onNavigation(direction, tab))
                    this.setFocused(this.list.getCurrentTab().container);
            } else {
                boolean result = this.list.getCurrentTab().container.onNavigation(direction, tab);
                if (!result)
                    this.setFocused(this.list);
            }
            return true;
        } else {
            if (!this.isFocused()) {
                this.setFocused(true);
                this.setFocused(this.isLeft ? this.list : this.list.getCurrentTab().container);
            } else {
                this.isLeft = this.getFocused() == this.list;
            }

            if (this.getFocused() == null) {
                this.setFocused(this.isLeft ? this.list : this.list.getCurrentTab().container);
            }

            return this.getFocused().onNavigation(direction, tab);
        }
    }

    /* Render */

    @Override
    protected void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (this.title != null) {
            drawCenteredText(matrices, this.client.textRenderer, this.title, this.getX() + this.list.getWidth() / 2, this.getY() + 6, 0xffffffff);
        }
        this.list.render(matrices, mouseX, mouseY, delta);
        if (this.list.getCurrentTab() != null)
            this.list.getCurrentTab().container.render(matrices, mouseX, mouseY, delta);
    }

    public static abstract class Entry extends SpruceEntryListWidget.Entry implements WithBackground {
        protected final SideTabList parent;
        private final Text title;
        private Background background = EmptyBackground.EMPTY_BACKGROUND;

        protected Entry(SideTabList parent, Text title) {
            this.parent = parent;
            this.title = title;
        }

        @Override
        public int getWidth() {
            return this.parent.getInnerWidth();
        }

        /**
         * Returns the title of this entry.
         *
         * @return the title
         */
        public Text getTitle() {
            return this.title;
        }

        @Override
        public @NotNull Background getBackground() {
            return this.background;
        }

        @Override
        public void setBackground(@NotNull Background background) {
            this.background = background;
        }

        /* Rendering */

        @Override
        protected void renderBackground(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            this.getBackground().render(matrices, this, 0, mouseX, mouseY, delta);
        }
    }

    public static class TabEntry extends Entry {
        private final List<OrderedText> description;
        private final AbstractSpruceWidget container;
        private boolean selected;

        protected TabEntry(SideTabList parent, Text title, @Nullable Text description, AbstractSpruceWidget container) {
            super(parent, title);
            if (description == null) this.description = null;
            else this.description = this.client.textRenderer.wrapLines(description, this.parent.getWidth() - 18);
            this.container = container;

            if (container instanceof SpruceEntryListWidget<?>) {
                ((SpruceEntryListWidget<?>) container).setAllowOutsideHorizontalNavigation(true);
            }
        }

        @Override
        public int getHeight() {
            return 8 + this.client.textRenderer.fontHeight
                    + (this.description == null ? 0 : this.description.size() * this.client.textRenderer.fontHeight + 4) + 4;
        }

        public boolean isSelected() {
            return this.selected;
        }

        @Override
        public void setFocused(boolean focused) {
            super.setFocused(focused);
            if (focused)
                this.selected = true;
        }

        /* Input */

        @Override
        protected boolean onMouseClick(double mouseX, double mouseY, int button) {
            if (button == 0) {
                this.playDownSound();
                this.parent.setSelected(this);
                return true;
            }
            return false;
        }

        /* Render */

        @Override
        protected void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            DrawableHelper.drawTextWithShadow(matrices, this.client.textRenderer, this.getTitle(), this.getX() + 4, this.getY() + 5, 0xffffff);
            if (this.description != null) {
                int y = this.getY() + 8 + this.client.textRenderer.fontHeight;
                for (Iterator<OrderedText> it = this.description.iterator(); it.hasNext(); y += 9) {
                    OrderedText line = it.next();
                    this.client.textRenderer.draw(matrices, line, this.getX() + 8, y, 0xffffff);
                }
            }
        }

        @Override
        protected void renderBackground(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            super.renderBackground(matrices, mouseX, mouseY, delta);
            if (this.selected || this.isMouseHovered())
                fill(matrices, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight() - 4, 0x1affffff);
        }

        @Override
        public String toString() {
            return "SpruceTabbedWidget$TabEntry{" +
                    "title=" + this.getTitle() +
                    ", description=" + this.description +
                    ", position=" + this.getPosition() +
                    ", width=" + this.getWidth() +
                    ", height=" + this.getHeight() +
                    ", container=" + this.container +
                    ", selected=" + this.selected +
                    ", background=" + this.getBackground() +
                    '}';
        }
    }

    public static class SeparatorEntry extends Entry {
        private final SpruceSeparatorWidget separatorWidget;

        protected SeparatorEntry(SideTabList parent, Text title) {
            super(parent, title);
            this.separatorWidget = new SpruceSeparatorWidget(Position.of(this, 0, 0), this.getWidth(), title) {
                @Override
                public int getWidth() {
                    return SeparatorEntry.this.getWidth();
                }
            };
        }

        public SpruceSeparatorWidget getSeparatorWidget() {
            return this.separatorWidget;
        }

        @Override
        public int getHeight() {
            return this.separatorWidget.getHeight() + 4;
        }

        /* Navigation */

        @Override
        public boolean onNavigation(@NotNull NavigationDirection direction, boolean tab) {
            return this.separatorWidget.onNavigation(direction, tab);
        }

        /* Rendering */

        @Override
        protected void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            this.separatorWidget.render(matrices, mouseX, mouseY, delta);
        }

        @Override
        public String toString() {
            return "SpruceTabbedWidget$SeparatorEntry{" +
                    "title=" + this.getTitle() +
                    ", position=" + this.getPosition() +
                    ", width=" + this.getWidth() +
                    ", height=" + this.getHeight() +
                    ", background=" + this.getBackground() +
                    '}';
        }
    }

    public static class SideTabList extends SpruceEntryListWidget<Entry> {
        private TabEntry currentTab = null;

        protected SideTabList(@NotNull Position position, int width, int height) {
            super(position, width, height, 0, SpruceTabbedWidget.Entry.class);
            this.setRenderTransition(false);
        }

        public TabEntry getCurrentTab() {
            return this.currentTab;
        }

        @Override
        public void setFocused(boolean focused) {
            super.setFocused(focused);
            if (!focused)
                this.setSelected(this.currentTab);
        }

        public void setSelected(TabEntry tab) {
            if (this.currentTab != null)
                this.currentTab.selected = false;
            tab.setFocused(true);
            this.setFocused(tab);
            this.currentTab = tab;
        }

        public TabEntry addTabEntry(Text title, @Nullable Text description, AbstractSpruceWidget container) {
            TabEntry entry = new TabEntry(this, title, description, container);
            this.addEntry(entry);
            if (this.getCurrentTab() == null)
                this.setSelected(entry);
            return entry;
        }

        public SeparatorEntry addSeparatorEntry(Text title) {
            SeparatorEntry entry = new SeparatorEntry(this, title);
            this.addEntry(entry);
            return entry;
        }

        /* Navigation */

        @Override
        public boolean onNavigation(@NotNull NavigationDirection direction, boolean tab) {
            if (this.requiresCursor()) return false;
            SpruceTabbedWidget.Entry old = this.getFocused();
            boolean result = super.onNavigation(direction, tab);
            SpruceTabbedWidget.Entry focused = this.getFocused();
            if (result && old != focused && focused instanceof TabEntry) {
                this.setSelected((TabEntry) focused);
            }
            return result;
        }
    }

    public interface ContainerFactory {
        AbstractSpruceWidget build(int width, int height);
    }
}
