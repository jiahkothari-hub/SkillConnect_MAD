package com.skillconnect.app.data;

import com.skillconnect.app.models.Booking;
import com.skillconnect.app.models.PortfolioItem;
import com.skillconnect.app.models.Provider;
import com.skillconnect.app.models.Review;
import com.skillconnect.app.models.User;
import com.skillconnect.app.utils.DateTimeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * FICTIONAL sample data used in demo mode and by the admin "Seed demo data" action.
 * All names, phone numbers and emails are made up.
 */
public final class DemoData {

    public static final String DEMO_PASSWORD = "demo123";
    public static final String CUSTOMER_EMAIL = "priya@demo.com";
    public static final String PROVIDER_EMAIL = "rahul@demo.com";
    public static final String ADMIN_EMAIL = "admin@demo.com";

    public static final String CUSTOMER_ID = "demo_customer_priya";
    public static final String ADMIN_ID = "demo_admin";
    public static final String RAHUL_ID = "demo_rahul_sharma";

    private static final List<String> WEEKDAYS = Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat");
    private static final List<String> ALL_DAYS = Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
    private static final List<String> WEEKENDS_PLUS = Arrays.asList("Wed", "Thu", "Fri", "Sat", "Sun");

    private DemoData() {
    }

    public static List<User> users() {
        List<User> users = new ArrayList<>();
        User priya = new User(CUSTOMER_ID, "Priya Singh", CUSTOMER_EMAIL, "9876500001", User.ROLE_CUSTOMER);
        priya.setAddress("Andheri West, Mumbai");
        priya.setLatitude(19.1197);
        priya.setLongitude(72.8468);
        users.add(priya);

        User admin = new User(ADMIN_ID, "SkillConnect Admin", ADMIN_EMAIL, "9876500099", User.ROLE_ADMIN);
        users.add(admin);

        for (Provider p : providers()) {
            User u = new User(p.getProviderId(), p.getName(), p.getEmail(), p.getPhone(), User.ROLE_PROVIDER);
            u.setAddress(p.getAddress());
            u.setLatitude(p.getLatitude());
            u.setLongitude(p.getLongitude());
            users.add(u);
        }
        User karan = new User("demo_customer_karan", "Karan Malhotra", "karan@demo.com", "9876500002", User.ROLE_CUSTOMER);
        karan.setAddress("Andheri East, Mumbai");
        users.add(karan);
        User aditi = new User("demo_customer_aditi", "Aditi Sen", "aditi@demo.com", "9876500003", User.ROLE_CUSTOMER);
        aditi.setAddress("Jogeshwari, Mumbai");
        users.add(aditi);
        return users;
    }

    public static List<Provider> providers() {
        List<Provider> list = new ArrayList<>();
        list.add(provider(RAHUL_ID, "Rahul Sharma", "Electrician", CategoryData.HOME,
                "Andheri East, Mumbai", 19.1136, 72.8697, 300, "per visit", 4.8, 37, 4, true, true, WEEKDAYS, "09:00", "19:00",
                "Experienced electrician specialising in residential wiring, appliance installation and electrical repairs. Safety-first work with neat finishing and transparent pricing.",
                Arrays.asList("Wiring", "AC Installation", "Appliance Repair", "Electrical Maintenance")));
        list.add(provider("demo_aarav_mehta", "Aarav Mehta", "Guitar Teacher", CategoryData.EDUCATION,
                "Bandra West, Mumbai", 19.0596, 72.8295, 500, "per hour", 4.7, 24, 6, true, true, WEEKENDS_PLUS, "11:00", "20:00",
                "Acoustic and electric guitar lessons for beginners and intermediate players. Learn chords, strumming, fingerstyle and your favourite Bollywood songs.",
                Arrays.asList("Acoustic Guitar", "Electric Guitar", "Music Theory", "Trinity Exam Prep")));
        list.add(provider("demo_riya_kapoor", "Riya Kapoor", "Graphic Designer", CategoryData.CREATIVE,
                "Powai, Mumbai", 19.1176, 72.9060, 800, "per project", 4.9, 52, 5, true, true, WEEKDAYS, "10:00", "18:00",
                "Brand identity and social media designer. I help small businesses and creators look professional with logos, packaging and Instagram creatives.",
                Arrays.asList("Logo Design", "Branding", "Social Media Posts", "Packaging")));
        list.add(provider("demo_neha_patil", "Neha Patil", "Makeup Artist", CategoryData.CREATIVE,
                "Borivali West, Mumbai", 19.2307, 72.8567, 1500, "per session", 4.8, 41, 7, true, true, ALL_DAYS, "07:00", "21:00",
                "Certified bridal and party makeup artist. HD and airbrush makeup, hairstyling and saree draping at your doorstep.",
                Arrays.asList("Bridal Makeup", "Party Makeup", "Hairstyling", "Saree Draping")));
        list.add(provider("demo_kabir_shah", "Kabir Shah", "Video Editor", CategoryData.CREATIVE,
                "Malad West, Mumbai", 19.1874, 72.8484, 1000, "per project", 4.6, 18, 3, false, true, WEEKDAYS, "12:00", "22:00",
                "YouTube and Instagram Reels editor. Fast turnaround, trending transitions, captions and colour grading.",
                Arrays.asList("Reels Editing", "YouTube Editing", "Colour Grading", "Motion Graphics")));
        list.add(provider("demo_meera_joshi", "Meera Joshi", "Math Tutor", CategoryData.EDUCATION,
                "Goregaon East, Mumbai", 19.1663, 72.8526, 400, "per hour", 4.9, 33, 8, true, true, WEEKDAYS, "15:00", "20:00",
                "Maths tutor for classes 6–12 (CBSE, ICSE, State Board) with a focus on concepts and exam practice. Home or online sessions.",
                Arrays.asList("Algebra", "Geometry", "Calculus", "Board Exam Prep")));
        list.add(provider("demo_imran_qureshi", "Imran Qureshi", "Plumber", CategoryData.HOME,
                "Jogeshwari West, Mumbai", 19.1360, 72.8490, 250, "per visit", 4.5, 29, 10, true, true, ALL_DAYS, "08:00", "20:00",
                "Leak repairs, bathroom fittings, water tank cleaning and pipeline work. Quick response for emergencies.",
                Arrays.asList("Leak Repair", "Bathroom Fittings", "Tank Cleaning", "Pipeline Work")));
        list.add(provider("demo_sneha_iyer", "Sneha Iyer", "Yoga Instructor", CategoryData.LIFESTYLE,
                "Juhu, Mumbai", 19.1075, 72.8263, 600, "per session", 4.8, 21, 5, true, true, ALL_DAYS, "06:00", "10:00",
                "Hatha and Vinyasa yoga for all levels. Personal and small group sessions focusing on flexibility, strength and breathing.",
                Arrays.asList("Hatha Yoga", "Vinyasa", "Pranayama", "Prenatal Yoga")));
        list.add(provider("demo_vikram_desai", "Vikram Desai", "Web Developer", CategoryData.TECH,
                "Vile Parle East, Mumbai", 19.0990, 72.8440, 2500, "per project", 4.7, 15, 4, true, true, WEEKDAYS, "10:00", "19:00",
                "I build fast, mobile-friendly websites for shops, clinics and startups. Includes hosting setup and basic SEO.",
                Arrays.asList("HTML/CSS", "React", "WordPress", "SEO")));
        list.add(provider("demo_ananya_rao", "Ananya Rao", "Photographer", CategoryData.CREATIVE,
                "Santacruz West, Mumbai", 19.0810, 72.8410, 2000, "per session", 4.9, 46, 6, true, false, WEEKENDS_PLUS, "08:00", "20:00",
                "Portrait, product and event photographer. Natural light specialist with quick edited deliveries.",
                Arrays.asList("Portraits", "Product Shoots", "Events", "Photo Editing")));
        list.add(provider("demo_farhan_ali", "Farhan Ali", "Carpenter", CategoryData.HOME,
                "Kurla West, Mumbai", 19.0726, 72.8845, 350, "per visit", 4.4, 12, 12, false, false, WEEKDAYS, "09:00", "18:00",
                "Furniture repair, modular fittings, door and window work. Custom shelves and wardrobes on request.",
                Arrays.asList("Furniture Repair", "Modular Fitting", "Custom Shelves")));
        list.add(provider("demo_pooja_nair", "Pooja Nair", "Fitness Trainer", CategoryData.LIFESTYLE,
                "Chembur, Mumbai", 19.0522, 72.9005, 700, "per session", 4.6, 19, 4, true, true, ALL_DAYS, "06:00", "21:00",
                "Certified personal trainer for weight loss, strength training and functional fitness. Home or society gym sessions.",
                Arrays.asList("Weight Loss", "Strength Training", "HIIT", "Diet Guidance")));
        list.add(provider("demo_arjun_kulkarni", "Arjun Kulkarni", "Coding Tutor", CategoryData.EDUCATION,
                "Thane West, Thane", 19.2183, 72.9781, 600, "per hour", 4.8, 27, 5, true, true, WEEKENDS_PLUS, "16:00", "21:00",
                "Learn Python, Java and web development from scratch. College project guidance and interview preparation.",
                Arrays.asList("Python", "Java", "Web Development", "DSA")));
        list.add(provider("demo_tanvi_deshmukh", "Tanvi Deshmukh", "Home Baker", CategoryData.LIFESTYLE,
                "Dadar West, Mumbai", 19.0178, 72.8478, 450, "per order", 4.7, 38, 3, true, true, ALL_DAYS, "10:00", "20:00",
                "Eggless cakes, brownies and cookies baked fresh at home. Custom birthday and theme cakes with 2 days notice.",
                Arrays.asList("Custom Cakes", "Brownies", "Cookies", "Eggless Baking")));
        list.add(provider("demo_rohan_verma", "Rohan Verma", "Android Developer", CategoryData.TECH,
                "Powai, Mumbai", 19.1197, 72.9050, 3000, "per project", 4.6, 9, 3, true, false, WEEKDAYS, "11:00", "19:00",
                "Native Android apps in Java and Kotlin with Firebase backends. MVPs for startups and small businesses.",
                Arrays.asList("Java", "Kotlin", "Firebase", "UI Design")));
        list.add(provider("demo_zoya_khan", "Zoya Khan", "Mehendi Artist", CategoryData.CREATIVE,
                "Mira Road, Thane", 19.2813, 72.8557, 1200, "per session", 4.9, 58, 9, true, true, ALL_DAYS, "09:00", "21:00",
                "Bridal, Arabic and Indo-western mehendi designs using natural henna. Group bookings for sangeet and festivals.",
                Arrays.asList("Bridal Mehendi", "Arabic Designs", "Festive Mehendi")));
        list.add(provider("demo_devansh_gupta", "Devansh Gupta", "AC Repair", CategoryData.HOME,
                "Andheri West, Mumbai", 19.1300, 72.8300, 400, "per visit", 4.5, 22, 6, true, true, WEEKDAYS, "09:00", "19:00",
                "Split and window AC servicing, gas refilling, installation and uninstallation for all major brands.",
                Arrays.asList("AC Servicing", "Gas Refill", "Installation")));
        return list;
    }

    private static Provider provider(String id, String name, String title, String category, String address,
                                     double lat, double lng, double price, String unit, double rating, int reviews,
                                     int experience, boolean available, boolean verified, List<String> days,
                                     String from, String to, String description, List<String> skills) {
        Provider p = new Provider();
        p.setProviderId(id);
        p.setName(name);
        p.setTitle(title);
        p.setCategory(category);
        p.setAddress(address);
        p.setLatitude(lat);
        p.setLongitude(lng);
        p.setStartingPrice(price);
        p.setPriceUnit(unit);
        p.setAverageRating(rating);
        p.setReviewCount(reviews);
        p.setExperienceYears(experience);
        p.setAvailable(available);
        p.setVerified(verified);
        p.setAvailableDays(new ArrayList<>(days));
        p.setAvailableFrom(from);
        p.setAvailableTo(to);
        p.setDescription(description);
        p.setSkills(new ArrayList<>(skills));
        String first = name.split(" ")[0].toLowerCase();
        p.setEmail(first + "@demo.com");
        p.setPhone("98765" + String.format(java.util.Locale.US, "%05d", Math.abs(id.hashCode()) % 100000));
        p.setActive(true);
        p.setCreatedAt(System.currentTimeMillis());
        return p;
    }

    public static List<Review> reviews() {
        List<Review> list = new ArrayList<>();
        long day = 24L * 60 * 60 * 1000;
        long now = System.currentTimeMillis();
        list.add(review("r1", RAHUL_ID, "Karan Malhotra", 5, "Rahul fixed our wiring issue within an hour. Very professional and explained everything clearly.", now - 3 * day));
        list.add(review("r2", RAHUL_ID, "Aditi Sen", 5, "Installed 4 fans and 2 lights. Neat work and reasonable pricing.", now - 10 * day));
        list.add(review("r3", RAHUL_ID, "Siddharth Rao", 4, "Good work, arrived a little late but called ahead.", now - 25 * day));
        list.add(review("r4", "demo_aarav_mehta", "Ishaan Bose", 5, "My son loves his guitar classes. Aarav is patient and fun.", now - 6 * day));
        list.add(review("r5", "demo_aarav_mehta", "Nikita Shah", 4, "Great teacher, flexible timings.", now - 30 * day));
        list.add(review("r6", "demo_riya_kapoor", "Cafe Chai Tales", 5, "Riya designed our entire brand kit. Loved the logo!", now - 4 * day));
        list.add(review("r7", "demo_riya_kapoor", "Mehul Jain", 5, "Super quick and creative. Highly recommend for Instagram posts.", now - 14 * day));
        list.add(review("r8", "demo_neha_patil", "Sana Sheikh", 5, "Did my bridal makeup — lasted the whole day and looked natural.", now - 12 * day));
        list.add(review("r9", "demo_meera_joshi", "Rohit Menon", 5, "My daughter's maths score went from 62 to 88. Thank you ma'am!", now - 8 * day));
        list.add(review("r10", "demo_kabir_shah", "Tech With Tara", 4, "Clean edits and good captions. Slight delay on revisions.", now - 9 * day));
        list.add(review("r11", "demo_imran_qureshi", "Anil Kumar", 5, "Came within 30 minutes for a burst pipe. Lifesaver.", now - 2 * day));
        list.add(review("r12", "demo_zoya_khan", "Fatima Z.", 5, "Beautiful bridal mehendi, colour came out dark and rich.", now - 20 * day));
        return list;
    }

    private static Review review(String id, String providerId, String customer, double rating, String comment, long at) {
        Review r = new Review();
        r.setReviewId(id);
        r.setBookingId(id);
        r.setProviderId(providerId);
        r.setCustomerId("demo_" + customer.toLowerCase().replace(' ', '_'));
        r.setCustomerName(customer);
        r.setRating(rating);
        r.setComment(comment);
        r.setCreatedAt(at);
        return r;
    }

    public static List<PortfolioItem> portfolio() {
        List<PortfolioItem> list = new ArrayList<>();
        String[][] items = {
                {RAHUL_ID, "Modular switchboard installation"},
                {RAHUL_ID, "Living room false-ceiling lights"},
                {RAHUL_ID, "Main panel rewiring"},
                {"demo_riya_kapoor", "Cafe brand identity"},
                {"demo_riya_kapoor", "Instagram carousel series"},
                {"demo_neha_patil", "Bridal look — Maharashtrian wedding"},
                {"demo_neha_patil", "Engagement party makeup"},
                {"demo_kabir_shah", "Travel reel edit"},
                {"demo_ananya_rao", "Product shoot for a jewellery brand"},
                {"demo_zoya_khan", "Full-hand bridal design"},
                {"demo_tanvi_deshmukh", "Two-tier chocolate truffle cake"},
        };
        long now = System.currentTimeMillis();
        for (int i = 0; i < items.length; i++) {
            PortfolioItem item = new PortfolioItem();
            item.setPortfolioId("pf" + i);
            item.setProviderId(items[i][0]);
            item.setImageUrl(PortfolioItem.DEMO_PREFIX + i);
            item.setDescription(items[i][1]);
            item.setTimestamp(now - i * 3600_000L);
            list.add(item);
        }
        return list;
    }

    public static List<Booking> bookings() {
        List<Booking> list = new ArrayList<>();
        long day = 24L * 60 * 60 * 1000;
        long now = System.currentTimeMillis();
        List<Provider> providers = providers();
        Provider rahul = providers.get(0);
        Provider aarav = providers.get(1);
        Provider neha = providers.get(3);
        Provider meera = providers.get(5);

        list.add(booking("b1", CUSTOMER_ID, "Priya Singh", "9876500001", rahul, "Electrical Repair",
                "Kitchen socket sparks when the mixer is switched on. Please check and replace.",
                now - 3 * day, "17:30", Booking.STATUS_COMPLETED, "Andheri West, Mumbai"));
        list.add(booking("b2", CUSTOMER_ID, "Priya Singh", "9876500001", aarav, "Guitar Lesson",
                "Beginner lesson for me — I have an acoustic guitar already.",
                now + 2 * day, "18:00", Booking.STATUS_ACCEPTED, "Andheri West, Mumbai"));
        list.add(booking("b3", CUSTOMER_ID, "Priya Singh", "9876500001", meera, "Math Tutoring",
                "Class 10 algebra revision before board exams, 2 sessions per week.",
                now + 5 * day, "16:00", Booking.STATUS_PENDING, "Andheri West, Mumbai"));
        list.add(booking("b4", CUSTOMER_ID, "Priya Singh", "9876500001", neha, "Party Makeup",
                "Makeup for a cousin's engagement.",
                now - 12 * day, "10:00", Booking.STATUS_CANCELLED, "Andheri West, Mumbai"));
        // Requests received by the demo provider Rahul
        list.add(booking("b5", "demo_customer_karan", "Karan Malhotra", "9876500002", rahul, "Light Installation",
                "I need help installing 3 ceiling lights in the living room.",
                now + day, "11:00", Booking.STATUS_PENDING, "Andheri East, Mumbai"));
        list.add(booking("b6", "demo_customer_aditi", "Aditi Sen", "9876500003", rahul, "MCB Repair",
                "MCB keeps tripping whenever the geyser is switched on.",
                now + 2 * day, "09:30", Booking.STATUS_PENDING, "Jogeshwari, Mumbai"));
        list.add(booking("b7", "demo_customer_sid", "Siddharth Rao", "9876500004", rahul, "Fan Installation",
                "Install two ceiling fans in bedrooms.",
                now + 3 * day, "15:00", Booking.STATUS_ACCEPTED, "Marol, Andheri East"));
        return list;
    }

    private static Booking booking(String id, String customerId, String customerName, String customerPhone,
                                   Provider p, String service, String desc, long dayMillis, String time,
                                   String status, String location) {
        Booking b = new Booking();
        b.setBookingId(id);
        b.setCustomerId(customerId);
        b.setCustomerName(customerName);
        b.setCustomerPhone(customerPhone);
        b.setProviderId(p.getProviderId());
        b.setProviderName(p.getName());
        b.setProviderPhone(p.getPhone());
        b.setService(service);
        b.setDescription(desc);
        b.setDate(DateTimeUtils.formatDate(dayMillis));
        b.setTime(time);
        b.setScheduledAt(DateTimeUtils.combine(dayMillis, time));
        b.setPrice(p.getStartingPrice());
        b.setLocation(location);
        b.setStatus(status);
        b.setCreatedAt(System.currentTimeMillis() - 5 * 24 * 3600_000L);
        b.setUpdatedAt(b.getCreatedAt());
        return b;
    }
}
