package com.knifemod.client;

import com.knifemod.KnifeDef;
import com.knifemod.KnifeItems;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Client-side only. While a knife is "drawn", it sits in your selected hotbar slot on this
 * client, so the game's own first-person renderer shows it in your hand. The server is never
 * told, and the original stack is restored when you put the knife away.
 */
public final class KnifeState {
    public static final List<KnifeDef> OWNED = new ArrayList<>();
    public static KnifeDef equipped;

    private static boolean inHand = false;
    private static int heldSlot = -1;
    private static ItemStack savedStack = ItemStack.EMPTY;
    private static ItemStack heldStack = ItemStack.EMPTY;
    private static KnifeDef heldKnife;

    private KnifeState() {
    }

    public static boolean isInHand() {
        return inHand;
    }

    public static void toggle(PlayerInventory inv) {
        if (inHand) {
            putAway(inv);
        } else {
            draw(inv);
        }
    }

    /** Only works from an empty main hand, as requested. */
    public static void draw(PlayerInventory inv) {
        if (equipped == null || inHand) return;
        if (!inv.getStack(inv.getSelectedSlot()).isEmpty()) return;
        heldSlot = inv.getSelectedSlot();
        savedStack = inv.getStack(heldSlot);
        place(inv, equipped);
        inHand = true;
    }

    public static void putAway(PlayerInventory inv) {
        if (inHand && heldSlot >= 0 && inv.getStack(heldSlot) == heldStack) {
            inv.setStack(heldSlot, savedStack);
        }
        clear();
    }

    /** Call every client tick. */
    public static void tick(PlayerInventory inv) {
        if (!inHand) return;

        // Player moved to another hotbar slot: put the knife back where it came from
        if (inv.getSelectedSlot() != heldSlot) {
            putAway(inv);
            return;
        }

        // The server changed that slot (for example, it picked something up): stop holding the knife
        if (inv.getStack(heldSlot) != heldStack) {
            clear();
            return;
        }

        // Switched which knife is equipped in the case menu: swap it in our hand
        if (equipped != null && equipped != heldKnife) {
            place(inv, equipped);
        }
    }

    private static void place(PlayerInventory inv, KnifeDef knife) {
        heldKnife = knife;
        heldStack = KnifeItems.createStack(knife);
        inv.setStack(heldSlot, heldStack);
    }

    private static void clear() {
        inHand = false;
        heldSlot = -1;
        savedStack = ItemStack.EMPTY;
        heldStack = ItemStack.EMPTY;
        heldKnife = null;
    }

    /** Best rarity first, then by name. */
    public static final Comparator<KnifeDef> BY_RARITY =
            Comparator.comparingInt((KnifeDef k) -> k.rarity().ordinal()).reversed().thenComparing(KnifeDef::name);

    public static void equip(KnifeDef knife) {
        equipped = knife;
    }

    public static void equipNext() {
        if (OWNED.isEmpty()) return;
        int i = OWNED.indexOf(equipped);
        equipped = OWNED.get((i + 1) % OWNED.size());
    }
}
