#!/usr/bin/env python3
"""Generate a professional PDF brief from TinyTroupe focus group results."""
import json
from pathlib import Path
from datetime import datetime

from reportlab.lib.pagesizes import A4
from reportlab.lib.units import cm
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.colors import HexColor
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle,
    PageBreak, KeepTogether
)
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY

# Paths
OUTPUT = Path("/Users/dalerogers/20-INDIE/products/SMOLCASE/tinytroupe-workshop/output")
RESULTS = json.loads((OUTPUT / "demo_results.json").read_text())
PDF_PATH = OUTPUT / "focus_group_brief.pdf"

# ---------------------------------------------------------------------------
# Styles
# ---------------------------------------------------------------------------
styles = getSampleStyleSheet()

# Custom colors
NAVY = HexColor('#1a2a4a')
SLATE = HexColor('#4a5568')
LIGHT_GRAY = HexColor('#f7f8fa')
DARK_TEXT = HexColor('#2d3748')
ACCENT = HexColor('#c53030')
MUTED = HexColor('#718096')

# Base style
base = ParagraphStyle(
    'Base',
    parent=styles['Normal'],
    fontName='Helvetica',
    fontSize=10,
    leading=14,
    textColor=DARK_TEXT,
    spaceAfter=6,
)

# Title
title_style = ParagraphStyle(
    'BriefTitle',
    parent=base,
    fontName='Helvetica-Bold',
    fontSize=26,
    leading=32,
    textColor=NAVY,
    alignment=TA_CENTER,
    spaceAfter=8,
)

# Subtitle
subtitle_style = ParagraphStyle(
    'BriefSubtitle',
    parent=base,
    fontName='Helvetica',
    fontSize=13,
    leading=18,
    textColor=SLATE,
    alignment=TA_CENTER,
    spaceAfter=24,
)

# Meta line (date, etc.)
meta_style = ParagraphStyle(
    'BriefMeta',
    parent=base,
    fontName='Helvetica',
    fontSize=9,
    leading=12,
    textColor=MUTED,
    alignment=TA_CENTER,
    spaceAfter=12,
)

# Section heading
section_style = ParagraphStyle(
    'SectionHeading',
    parent=base,
    fontName='Helvetica-Bold',
    fontSize=14,
    leading=18,
    textColor=NAVY,
    spaceBefore=18,
    spaceAfter=10,
)

# Sub-section heading
sub_section_style = ParagraphStyle(
    'SubSectionHeading',
    parent=base,
    fontName='Helvetica-Bold',
    fontSize=11,
    leading=14,
    textColor=DARK_TEXT,
    spaceBefore=12,
    spaceAfter=6,
)

# Body
body_style = ParagraphStyle(
    'BriefBody',
    parent=base,
    fontName='Helvetica',
    fontSize=10,
    leading=14,
    textColor=DARK_TEXT,
    alignment=TA_JUSTIFY,
    spaceAfter=8,
)

# Bullet item
bullet_style = ParagraphStyle(
    'BulletItem',
    parent=base,
    fontName='Helvetica',
    fontSize=10,
    leading=13,
    textColor=DARK_TEXT,
    leftIndent=18,
    spaceAfter=4,
)

# Quote
quote_style = ParagraphStyle(
    'Quote',
    parent=base,
    fontName='Helvetica-Oblique',
    fontSize=9.5,
    leading=13,
    textColor=SLATE,
    leftIndent=24,
    rightIndent=24,
    spaceBefore=6,
    spaceAfter=6,
    borderWidth=0,
    borderColor=LIGHT_GRAY,
    borderPadding=8,
    backColor=LIGHT_GRAY,
)

# Sentiment badge text
sentiment_style = ParagraphStyle(
    'Sentiment',
    parent=base,
    fontName='Helvetica-Bold',
    fontSize=18,
    leading=22,
    textColor=ACCENT,
    spaceAfter=12,
)

# Small caption
caption_style = ParagraphStyle(
    'Caption',
    parent=base,
    fontName='Helvetica',
    fontSize=8,
    leading=10,
    textColor=MUTED,
    spaceAfter=4,
)

# ---------------------------------------------------------------------------
# Build document
# ---------------------------------------------------------------------------
doc = SimpleDocTemplate(
    str(PDF_PATH),
    pagesize=A4,
    topMargin=2.5*cm,
    bottomMargin=2.5*cm,
    leftMargin=2.8*cm,
    rightMargin=2.5*cm,
)

story = []

# ---- Cover -----------------------------------------------------------------
story.append(Spacer(1, 3*cm))
story.append(Paragraph("FOCUS GROUP BRIEF", meta_style))
story.append(Spacer(1, 1.5*cm))
story.append(Paragraph("AI Developer Tools Evaluation", title_style))
story.append(Paragraph("Simulated Focus Group Report — TinyTroupe", subtitle_style))
story.append(Spacer(1, 0.8*cm))
story.append(Paragraph(f"Generated {datetime.now().strftime('%B %d, %Y')}", meta_style))
story.append(Spacer(1, 0.3*cm))
story.append(Paragraph("4 personas | 8 simulation steps | Mixed sentiment", meta_style))
story.append(PageBreak())

# ---- Executive Summary ------------------------------------------------------
story.append(Paragraph("Executive Summary", section_style))

sentiment = RESULTS['sentiment'].upper()
story.append(Paragraph(f"Overall Sentiment: <b>{sentiment}</b>", sentiment_style))

summary_text = (
    "A simulated focus group of three domain experts evaluated an AI-powered "
    "development environment offering inline predictions, auto-generated tests, "
    "and legacy-code explanations via natural-language chat. The group converged "
    "on a nuanced 'conditional yes': the tool is valuable only if designed with "
    "structural safeguards against over-trust. All three participants identified "
    "the same core risk — fluent-but-wrong output that erases the verification "
    "moment that normally transfers institutional knowledge. The critical design "
    "property identified was <i>trust calibration</i>: making uncertainty visible, "
    "not just accurate. This is an interface problem, not a model problem."
)
story.append(Paragraph(summary_text, body_style))
story.append(Spacer(1, 8))

# Quick facts table
facts = [
    ['Activity', RESULTS['simulation_name']],
    ['Format', 'Focus Group (moderated)'],
    ['Participants', ', '.join(RESULTS['agents'])],
    ['Simulation Steps', str(RESULTS['steps_run'])],
    ['Dialogue Turns', str(RESULTS.get('dialogue_turns', 'N/A'))],
    ['Sentiment', sentiment],
]
facts_table = Table(facts, colWidths=[5*cm, 10*cm])
facts_table.setStyle(TableStyle([
    ('FONTNAME', (0, 0), (0, -1), 'Helvetica-Bold'),
    ('FONTNAME', (1, 0), (1, -1), 'Helvetica'),
    ('FONTSIZE', (0, 0), (-1, -1), 9.5),
    ('TEXTCOLOR', (0, 0), (-1, -1), DARK_TEXT),
    ('TOPPADDING', (0, 0), (-1, -1), 5),
    ('BOTTOMPADDING', (0, 0), (-1, -1), 5),
    ('LEFTPADDING', (0, 0), (-1, -1), 8),
    ('RIGHTPADDING', (0, 0), (-1, -1), 8),
    ('LINEBELOW', (0, 0), (-1, -2), 0.5, HexColor('#e2e8f0')),
    ('BACKGROUND', (0, 0), (-1, -1), LIGHT_GRAY),
]))
story.append(facts_table)
story.append(Spacer(1, 12))

# ---- Key Findings -----------------------------------------------------------
story.append(Paragraph("Key Findings", section_style))
for i, finding in enumerate(RESULTS.get('findings', []), 1):
    story.append(Paragraph(f"{i}. {finding}", bullet_style))
story.append(Spacer(1, 8))

# Pull quote from the rich dialogue
story.append(Paragraph(
    "The metric I would care about is not time-to-any-answer; it is time-to-correct-action. "
    "If the tool makes juniors faster at producing confident mistakes, that is negative DX with excellent typography.",
    quote_style
))
story.append(Paragraph("— Avery Williams, DX Engineer", caption_style))
story.append(Spacer(1, 8))

# ---- Top Ideas & Appeals ----------------------------------------------------
story.append(Paragraph("Top Ideas & Appeals", section_style))
for i, idea in enumerate(RESULTS.get('ideas', []), 1):
    story.append(Paragraph(f"{i}. {idea}", bullet_style))
story.append(Spacer(1, 8))

# ---- Dissent & Concerns -----------------------------------------------------
story.append(Paragraph("Dissent & Concerns", section_style))
for i, concern in enumerate(RESULTS.get('dissent', []), 1):
    story.append(Paragraph(f"{i}. {concern}", bullet_style))
story.append(Spacer(1, 8))

story.append(Paragraph(
    "The failure doesn't surface in the sprint review. It surfaces in May, when three hundred "
    "claimants get incorrect payments, and it surfaces on a phone line, to a call handler who "
    "has no idea anything changed, let alone why. In service terms, the cost of the confident "
    "error is exported to the people least equipped to absorb it. The developer's day got easier. "
    "The system as a whole got worse.",
    quote_style
))
story.append(Paragraph("— Elena Varga, Service Designer", caption_style))
story.append(PageBreak())

# ---- Participant Profiles ---------------------------------------------------
story.append(Paragraph("Participant Profiles", section_style))

participants = [
    (
        "Jordan Okafor",
        "Staff Software Engineer, Fintech Platform",
        "Designs systems handling millions of transactions. Reviews architecture RFCs, mentors seniors, "
        "still writes code. Calm during incidents, ruthless in post-mortems. Values observability over optimism. "
        "Key concern: fluent prose reads as calibrated confidence; juniors lack scar tissue to know which questions are load-bearing."
    ),
    (
        "Avery Williams",
        "Developer Experience Engineer",
        "Builds tools, docs, and workflows that make engineers productive. Measures time-to-first-PR, "
        "API error rates, documentation findability. Believes great DX is a competitive moat. "
        "Key concern: the core DX failure is a UI that makes wrong output feel already reviewed, lowering activation energy for trust."
    ),
    (
        "Elena Varga",
        "Lead Service Designer, Government Digital Service",
        "Designs end-to-end services across channels. Maps journeys, identifies failure demand, "
        "redesigns bureaucratic processes. Cares about equity and accessibility. "
        "Key concern: the cost of confident error is exported to citizens and frontline staff — the developer's day gets easier while the system gets worse."
    ),
]

for name, role, desc in participants:
    story.append(Paragraph(name, sub_section_style))
    story.append(Paragraph(f"<i>{role}</i>", caption_style))
    story.append(Paragraph(desc, body_style))
    story.append(Spacer(1, 4))

story.append(Spacer(1, 12))

# ---- Methodology ------------------------------------------------------------
story.append(Paragraph("Methodology", section_style))
story.append(Paragraph(
    "This brief was generated using TinyTroupe, a multi-agent persona simulation framework. "
    "Four personas were defined programmatically with detailed personality traits, preferences, "
    "and Big Five profiles. An 8-step focus group activity was moderated by a neutral facilitator "
    "within a shared TinyWorld environment. Dialogue was extracted from the simulation log and "
    "synthesized into structured findings, ideas, and dissent points. No human participants were involved; "
    "all responses reflect the emergent behavior of the simulated personas.",
    body_style
))
story.append(Spacer(1, 8))
story.append(Paragraph(
    "<b>Limitations:</b> Simulated personas may amplify or homogenize perspectives based on prompt framing. "
    "Results should be treated as directional hypotheses rather than empirical evidence. Cost tracking "
    "was unavailable for this run.",
    body_style
))

# ---- Footer -----------------------------------------------------------------
def header_footer(canvas, doc):
    canvas.saveState()
    width, height = A4
    canvas.setFont('Helvetica', 8)
    canvas.setFillColor(MUTED)
    canvas.drawCentredString(width / 2, 1.2*cm, f"Focus Group Brief — Page {doc.page}")
    canvas.restoreState()

def first_page(canvas, doc):
    pass

# Build
doc.build(story, onFirstPage=first_page, onLaterPages=header_footer)
print(f"✅ PDF generated: {PDF_PATH}")
print(f"   Size: {PDF_PATH.stat().st_size:,} bytes")
