from playwright.sync_api import sync_playwright
from pathlib import Path

html_path = Path("/Users/dalerogers/Library/Application Support/kimi-desktop/daimon-share/daimon/agents/main/blueprint/widgets/widget_e0501433-23f2-4e41-9922-158dfe08824c/workspace/index.html")
out_path = Path("/Users/dalerogers/20-INDIE/products/SMOLCASE/tinytroupe-workshop/output/dashboard_screenshot.png")

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={"width": 800, "height": 1200})
    page.goto(f"file://{html_path}")
    page.wait_for_timeout(3000)
    page.screenshot(path=str(out_path))
    browser.close()

print(f"Screenshot saved: {out_path}")
print(f"Size: {out_path.stat().st_size} bytes")
