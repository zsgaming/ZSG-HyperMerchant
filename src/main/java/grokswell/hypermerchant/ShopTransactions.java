package grokswell.hypermerchant;

//import static java.lang.System.out;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import regalowl.hyperconomy.api.HyperAPI;
import regalowl.hyperconomy.HyperConomy;
import regalowl.hyperconomy.HyperEconomy;
import regalowl.hyperconomy.api.HyperEconAPI;
import regalowl.hyperconomy.hyperobject.HyperObject;
import regalowl.hyperconomy.hyperobject.HyperObjectType;
import regalowl.hyperconomy.account.HyperPlayer;
import regalowl.hyperconomy.shop.PlayerShop;
import regalowl.hyperconomy.transaction.TransactionResponse;

public class ShopTransactions {
	ArrayList<ArrayList<String>> pages = new ArrayList<ArrayList<String>>();
	int items_count;
	String shopname;
	private Player player;
	private HyperConomy hc;
	//private LanguageFile hc_lang;
    MerchantMenu shopmenu;
    HyperPlayer hp;
	HyperAPI hyperAPI = new HyperAPI();
	HyperEconAPI heAPI = new HyperEconAPI();
	
	public ShopTransactions(Player plyr, String sname, HyperMerchantPlugin plgn, MerchantMenu sm) {
		player=plyr;
		shopname=sname;
		shopmenu = sm;
		hc = HyperConomy.hc;
		hp = hyperAPI.getHyperPlayer(player.getName());
		//hc_lang = hc.getLanguageFile();

	}
	
	//PLAYER SELLS ENCHANT TO SHOP
	public HyperObject Sell(String enchant) {

		HyperObject ho = hyperAPI.getHyperObject(enchant,hyperAPI.getShop(shopname).getEconomy());
		if ((hyperAPI.getShop(shopname).isTradeable(ho))) {
			TransactionResponse response = hyperAPI.sell(player, ho, 1, hyperAPI.getShop(shopname));
			response.sendMessages();
			if (response.successful()){
				return ho;
			}
		}
		
		
		return null;
	}
	
	//PLAYER SELLS ITEM TO SHOP
	public HyperObject Sell(ItemStack item_stack) {
			
		int item_amount = item_stack.getAmount();
		HyperObject ho = hyperAPI.getHyperObject(item_stack, hyperAPI.getShop(shopname).getEconomy(), hyperAPI.getShop(shopname));
		if (ho==null) {
			return null;
		}
		String item_name = ho.getDisplayName().toLowerCase();
		ho = hyperAPI.getHyperObject(item_name, hyperAPI.getShop(shopname).getEconomy(), hyperAPI.getShop(shopname));
		
		if (shopmenu.getShopStock().display_names.contains(item_name) && (hyperAPI.getShop(shopname).isTradeable(ho))) {
			player.getInventory().addItem(item_stack);
			TransactionResponse response = hyperAPI.sell(player, ho, item_amount, hyperAPI.getShop(shopname));
			response.sendMessages();
			return ho;
		}
		
		
		return null;
	}
	
	//PLAYER BUYS ITEM FROM SHOP
	public HyperObject Buy(String item, int qty, double commission) {
		HyperObject ho = hyperAPI.getHyperObject(item.replaceAll(" ", "_"), hyperAPI.getShop(shopname).getEconomy(), hyperAPI.getShop(shopname));
		if (!hp.hasBuyPermission(hyperAPI.getShop(shopname))) {
			player.sendMessage("You cannot buy from this shop.");
			return null;
		}
		
		TransactionResponse response = hyperAPI.buy(player, ho, qty, hyperAPI.getShop(shopname));
		
		if (hyperAPI.getPlayerShopList().contains(shopname) && commission > 0.0) {
		    if (ho.isShopObject()){
			    double amount = ho.getBuyPrice()*commission;
			    String owner_name = hyperAPI.getShop(shopname).getOwner().getName();
				heAPI.withdraw(amount, Bukkit.getPlayer(owner_name));
				heAPI.depositAccount(amount, owner_name);
		    }
		}

		return ho;
	}
	
	
	//PLAYER-MANAGER REMOVES ITEMS FROM SHOP
	public HyperObject Remove(String item, int qty) {
		HyperObject ho = hyperAPI.getHyperObject(item.replaceAll(" ", "_"), hyperAPI.getShop(shopname).getEconomy(), hyperAPI.getShop(shopname));
		
		if (ho.getType() == HyperObjectType.ENCHANTMENT) {
			if (player.getInventory().contains(Material.BOOK)) {
				ItemStack ebook = new ItemStack(Material.ENCHANTED_BOOK);
				EnchantmentStorageMeta im = (EnchantmentStorageMeta) ebook.getItemMeta();
				im.addStoredEnchant(ho.getEnchantment(), ho.getEnchantmentLevel(), true);
				ebook.setItemMeta(im);
				HashMap<Integer, ItemStack> excess = player.getInventory().addItem(ebook);
				//out.println("EXCESS: "+excess);
				if (excess.size() > 0) {
					player.sendMessage(ChatColor.YELLOW+"Your inventory is full.");
					return null;
				} else {
					ho.setStock(ho.getStock() - 1);
					//out.println("ho stock: "+ho.getStock());
					int index = player.getInventory().first(Material.BOOK);
					ItemStack book = player.getInventory().getItem(index);
					if (book.getAmount()==1){
						player.getInventory().clear(index);
					} else {
						book.setAmount(book.getAmount()-1);
						player.getInventory().setItem(index, book);
					}
					return ho;
				}
			}
			else {
				player.sendMessage(ChatColor.YELLOW+"You must have a book to take an enchantment from your shop.");
				return null;
			}
			
		}
		
		else if (ho.getType() == HyperObjectType.ITEM) {
			int space = ho.getAvailableSpace(player.getInventory());
			int qty_space = qty/ho.getItemStack().getMaxStackSize()+1;
			if (space < qty_space) {
				player.sendMessage("You haven't got enough space in your inventory to take "+qty+" "+ho.getDisplayName());
				return null;
			}
			if (ho.getStock() < 1) {
				return ho;
			}
			if (1 <= ho.getStock() && ho.getStock() < qty) {
				qty=(int) ho.getStock();
			}
			
			ho.add(qty, player.getInventory());
			ho.setStock(ho.getStock() - qty);
		}
		return ho;
	}	
	
	//PLAYER-MANAGER ADDS SOMETHING TO SHOP
	public ItemStack AddItemStack(ItemStack item_stack) {
		//out.println("AddItemStack: "+item_stack);
		PlayerShop pshop=hyperAPI.getPlayerShop(this.shopname);
		HyperEconomy he = hc.getDataManager().getEconomy(pshop.getEconomy());
		HyperObject ho = hp.getHyperEconomy().getHyperObject(item_stack);
		
		if (item_stack.getType()==Material.ENCHANTED_BOOK) {
			ItemStack return_item = this.addEnchantedBook(item_stack);
			return return_item;
		}
		
		ItemStack item = AddEnchantedItem(item_stack);
		if (item==null) {
			//out.println("item==null");
			item = AddItem(item_stack);
			if (item != null) {
				//out.println("item!=null");
				return item;
			}
		}
		
		HyperObject ho2 = hp.getHyperEconomy().getHyperObject(item);
		if (ho2 == null){
			//out.println("ho2==null");
			return item_stack;
		}
		else {
			//out.println("else");
			return item;
		}
	}	
	
	//PLAYER-MANAGER ADDS ITEMS TO SHOP
	public ItemStack AddItem(ItemStack item_stack) {
		//out.println("AddItem: "+item_stack);
		PlayerShop pshop=hyperAPI.getPlayerShop(this.shopname);
		HyperEconomy he = hc.getDataManager().getEconomy(pshop.getEconomy());
		HyperObject ho = hp.getHyperEconomy().getHyperObject(item_stack);

		
		int amount = item_stack.getAmount();
		int globalMaxStock = hc.getConf().getInt("shop.max-stock-per-item-in-playershops");
		HyperObject ho2 = he.getHyperObject(ho.getName(), pshop);
		if (ho2.getStock() + amount > globalMaxStock) {
			player.sendMessage("CANT_ADD_MORE_STOCK");
			return item_stack;
		}
		if (ho2.getType() == HyperObjectType.ITEM) {
			ho2.setStock(ho2.getStock() + amount);
			player.sendMessage("You added "+amount+" "+ho2.getDisplayName()+" to the shop "+this.shopname);
			return new ItemStack(Material.AIR);
		}
		return null;
	}
	
	//PLAYER-MANAGER ADDS ITEMS TO SHOP
	public ItemStack AddEnchantedItem(ItemStack item_stack) {
		//out.println("AddEcnhantedItem: "+item_stack);
		//make list of hyperconomy enchantment names from item's enchants
		ArrayList<HyperObject> enchants = new ArrayList<HyperObject>();
		for (HyperObject hob : hyperAPI.getEnchantmentHyperObjects(item_stack, player.getName())) {
			if (!hyperAPI.getShop(shopname).isBanned(hob)){
				enchants.add(hob);
				//out.println("enchant: "+hob.getDisplayName());
			}
		}
		
		if (enchants.size() < 1) {
			return null;
		}
		
		ArrayList<HyperObject> keep_enchant = new ArrayList<HyperObject>();
		//player.setItemInHand(player.getItemOnCursor().clone());
		for (HyperObject e : enchants) {
        	String ename = this.AddEnchant(e.getDisplayName());
            if (ename.equals(e)) {
    			//out.println("ELSE");
        		keep_enchant.add(e);
    		}
        }
		
		ItemStack stack = new ItemStack(item_stack.getType());
		if (keep_enchant.size()>0) {
			for (HyperObject e : keep_enchant){
				stack.addUnsafeEnchantment(e.getEnchantment(), e.getEnchantmentLevel());
			}
			player.sendMessage(ChatColor.YELLOW+"This shop doesn't want these enchantments: "+keep_enchant.toString());
		}
		
		return stack;
	}
	
	public ItemStack addEnchantedBook(ItemStack ebook) {
		//out.println("AddEnchantedBook: "+ebook);
		//make list of hyperconomy enchantment names from item's enchants
		ArrayList<HyperObject> enchants = new ArrayList<HyperObject>();
		for (HyperObject hob : hyperAPI.getEnchantmentHyperObjects(ebook, player.getName())) {
			if (!hyperAPI.getShop(shopname).isBanned(hob)){
				enchants.add(hob);
				//out.println("enchant: "+hob.getDisplayName());
			}
		}
		
		ArrayList<HyperObject> keep_enchant = new ArrayList<HyperObject>();
		//player.setItemInHand(player.getItemOnCursor().clone());
		for (HyperObject e : enchants) {
        	String ename = this.AddEnchant(e.getDisplayName());
            if (ename.equals(e)) {
    			//out.println("ELSE");
        		keep_enchant.add(e);
    		}
        }
		
		ItemStack stack = new ItemStack(Material.BOOK);
		if (keep_enchant.size()>0) {
			stack = new ItemStack(Material.ENCHANTED_BOOK);
			EnchantmentStorageMeta im = (EnchantmentStorageMeta) stack.getItemMeta();
			for (HyperObject e : keep_enchant){
				im.addStoredEnchant(e.getEnchantment(), e.getEnchantmentLevel(), true);
			}
			stack.setItemMeta(im);
			player.sendMessage(ChatColor.YELLOW+"This shop doesn't want these enchantments: "+keep_enchant.toString());
		}
		
		return stack;
	}
	
	//PLAYER-MANAGER ADDS ENCHANT TO SHOP
	public String AddEnchant(String enchant) {
		//out.println("AddEnchant: "+enchant);
		PlayerShop pshop=hyperAPI.getPlayerShop(this.shopname);
		HyperEconomy he = hc.getDataManager().getEconomy(pshop.getEconomy());
		HyperObject ho = hyperAPI.getHyperObject(enchant,hyperAPI.getShop(shopname).getEconomy());
		if (ho == null) {
			//out.println("ENCHANT NOT IN DB: "+enchant);
			return enchant;
		}
		int globalMaxStock = hc.getConf().getInt("shop.max-stock-per-item-in-playershops");
		HyperObject ho2 = he.getHyperObject(ho.getName(), pshop);
		if (ho2.getStock() + 1 > globalMaxStock) {
			player.sendMessage("CANT_ADD_MORE_STOCK");
			return enchant;
		} else {

			ho2.setStock(ho2.getStock() + 1);
			return "";
		}
	}	

}