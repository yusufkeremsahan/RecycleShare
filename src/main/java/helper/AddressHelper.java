package helper;

import java.util.*;

public class AddressHelper {
    // Şehir -> İlçeler haritası
    private static final Map<String, List<String>> CITIES = new TreeMap<>();

    static {
        // İstanbul
        CITIES.put("İstanbul", Arrays.asList("Kadıköy", "Beşiktaş", "Üsküdar", "Esenler", "Fatih", "Şişli", "Maltepe"));
        // Ankara
        CITIES.put("Ankara", Arrays.asList("Çankaya", "Keçiören", "Mamak", "Etimesgut", "Yenimahalle"));
        // İzmir
        CITIES.put("İzmir", Arrays.asList("Konak", "Bornova", "Karşıyaka", "Buca", "Çeşme"));
    }

    public static List<String> getCities() {
        return new ArrayList<>(CITIES.keySet());
    }

    public static List<String> getDistricts(String city) {
        return CITIES.getOrDefault(city, new ArrayList<>());
    }
}