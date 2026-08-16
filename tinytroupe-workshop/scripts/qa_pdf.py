import pypdfium2
from pathlib import Path

pdf_path = Path("/Users/dalerogers/20-INDIE/products/SMOLCASE/tinytroupe-workshop/output/focus_group_brief.pdf")
out_path = Path("/Users/dalerogers/20-INDIE/products/SMOLCASE/tinytroupe-workshop/output/pdf_qa_page1.png")

pdf = pypdfium2.PdfDocument(str(pdf_path))
print(f"PDF has {len(pdf)} pages")

for i in range(min(3, len(pdf))):
    page = pdf[i]
    bitmap = page.render(scale=2)
    img = bitmap.to_pil()
    out = out_path.parent / f"pdf_qa_page{i+1}.png"
    img.save(str(out))
    print(f"Saved {out}")

pdf.close()
print("QA render complete")
