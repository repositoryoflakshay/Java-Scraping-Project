package com.scraper;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.WaitUntilState;

class Senator {
    String Name;
    String Title;
    String Position;
    String Party;
    String Address;
    String Phone;
    String Email;
    String Url;
}

public class Main {
    public static void main(String[] args) {
        List<Senator> senators = new ArrayList<>();

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    // 🔍 Try headless(false) if site blocks bots
                    new BrowserType.LaunchOptions().setHeadless(false)
            );

            Page page = browser.newPage();

            // Navigate to page and wait for full load
            page.navigate("https://akleg.gov/senate.php",
                    new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));

            // 🔁 Retry logic: Use a more specific selector
            //
            //  ******************* THIS IS THE CORRECTED SECTION *******************
            //
            String[] selectors = {
                    "#leglist li > a:first-child",  // Target only the first 'a' tag in each 'li'
                    "ul.leglist li > a:first-child" // Same specific selector for the class
            };
            //
            //  *******************************************************************
            //

            List<ElementHandle> links = new ArrayList<>();
            for (String selector : selectors) {
                try {
                    page.waitForSelector(selector, new Page.WaitForSelectorOptions().setTimeout(20000));
                    links = page.querySelectorAll(selector);
                    if (!links.isEmpty()) {
                        System.out.println("✅ Found elements using selector: " + selector);
                        break;
                    }
                } catch (PlaywrightException e) {
                    System.out.println("⚠️ Selector failed: " + selector);
                }
            }

            if (links.isEmpty()) {
                System.out.println("❌ No senator links found. Try running with headless(false).");
                browser.close();
                return;
            }

            // Extract senator data
            for (ElementHandle link : links) {
                String name = link.innerText().trim();
                String title = "Senator";
                String url = link.getAttribute("href");
                if (url != null && url.startsWith("/")) {
                    url = "https://akleg.gov" + url;
                }

                Senator s = new Senator();
                s.Name = name;
                s.Title = title;
                s.Url = url;
                s.Position = "";
                s.Party = "";
                s.Address = "";
                s.Phone = "";
                s.Email = "";

                senators.add(s);
            }

            browser.close();
        }

        // Write output to JSON
        try (FileWriter writer = new FileWriter("senators.json")) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(senators, writer);
            System.out.println("✅ Data saved to senators.json successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}