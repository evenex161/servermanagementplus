package com.servermanagement.client;

import com.servermanagement.features.minebay.MineBayListing;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side cache for MineBay listings data
 */
public class ClientMineBayData {
    private static List<MineBayListing> cachedListings = new ArrayList<>();
    
    public static void updateListings(List<MineBayListing> listings) {
        cachedListings = new ArrayList<>(listings);
    }
    
    public static List<MineBayListing> getListings() {
        return new ArrayList<>(cachedListings);
    }
    
    public static void clear() {
        cachedListings.clear();
    }
}
