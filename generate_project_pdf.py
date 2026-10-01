import os
import sys
from reportlab.lib import colors
from reportlab.lib.pagesizes import letter
from reportlab.lib.units import inch
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, KeepTogether, HRFlowable
)
from reportlab.pdfgen import canvas

class NumberedCanvas(canvas.Canvas):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._saved_page_states = []

    def showPage(self):
        self._saved_page_states.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        num_pages = len(self._saved_page_states)
        for state in self._saved_page_states:
            self.__dict__.update(state)
            self.draw_page_decorations(num_pages)
            super().showPage()
        super().save()

    def draw_page_decorations(self, page_count):
        self.saveState()
        self.setFont("Helvetica", 9)
        self.setFillColor(colors.HexColor("#64748B"))
        
        # Header (pages > 1)
        if self._pageNumber > 1:
            self.drawString(54, 11 * inch - 36, "VizualX — Executive Project & Technical Report")
            self.setStrokeColor(colors.HexColor("#E2E8F0"))
            self.setLineWidth(0.75)
            self.line(54, 11 * inch - 42, 8.5 * inch - 54, 11 * inch - 42)
            
        # Footer
        footer_text = f"Page {self._pageNumber} of {page_count}"
        self.drawRightString(8.5 * inch - 54, 36, footer_text)
        self.drawString(54, 36, "CONFIDENTIAL — FOR HACKATHON EVALUATION & OPEN SOURCE ACCESS")
        self.setStrokeColor(colors.HexColor("#E2E8F0"))
        self.setLineWidth(0.75)
        self.line(54, 48, 8.5 * inch - 54, 48)
        self.restoreState()

def build_pdf(filename):
    doc = SimpleDocTemplate(
        filename,
        pagesize=letter,
        leftMargin=50,
        rightMargin=50,
        topMargin=46,
        bottomMargin=46
    )

    styles = getSampleStyleSheet()
    
    # Custom Palette
    c_primary = colors.HexColor("#0F172A")    # Deep Navy
    c_accent = colors.HexColor("#2563EB")     # Electric Blue
    c_secondary = colors.HexColor("#475569")  # Slate Gray
    c_bg_light = colors.HexColor("#F8FAFC")   # Soft Off-white
    c_border = colors.HexColor("#CBD5E1")     # Light Slate Border
    c_warning = colors.HexColor("#DC2626")    # Crimson Red
    c_success = colors.HexColor("#16A34A")    # Forest Green
    
    # Custom Typography Styles
    title_style = ParagraphStyle(
        'DocTitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=24,
        leading=28,
        textColor=c_primary,
        spaceAfter=3
    )
    
    subtitle_style = ParagraphStyle(
        'DocSubTitle',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=12,
        leading=15,
        textColor=c_accent,
        spaceAfter=10
    )

    meta_style = ParagraphStyle(
        'DocMeta',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=8.5,
        leading=12,
        textColor=c_secondary,
        spaceAfter=10
    )

    h1_style = ParagraphStyle(
        'Heading1_Custom',
        parent=styles['Heading1'],
        fontName='Helvetica-Bold',
        fontSize=14,
        leading=18,
        textColor=c_primary,
        spaceBefore=10,
        spaceAfter=6,
        keepWithNext=True
    )

    h2_style = ParagraphStyle(
        'Heading2_Custom',
        parent=styles['Heading2'],
        fontName='Helvetica-Bold',
        fontSize=11,
        leading=14.5,
        textColor=c_accent,
        spaceBefore=7,
        spaceAfter=3,
        keepWithNext=True
    )

    body_style = ParagraphStyle(
        'Body_Custom',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=9.5,
        leading=13.5,
        textColor=colors.HexColor("#1E293B"),
        spaceAfter=5
    )

    bullet_style = ParagraphStyle(
        'Bullet_Custom',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=9,
        leading=13,
        textColor=colors.HexColor("#334155"),
        leftIndent=12,
        spaceAfter=3.5
    )

    callout_style = ParagraphStyle(
        'Callout_Text',
        parent=styles['Normal'],
        fontName='Helvetica-Oblique',
        fontSize=9,
        leading=13,
        textColor=colors.HexColor("#0F172A")
    )

    table_header_style = ParagraphStyle(
        'TableHeader',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=8.5,
        leading=11,
        textColor=colors.white
    )

    table_body_style = ParagraphStyle(
        'TableBody',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=8,
        leading=10.5,
        textColor=colors.HexColor("#1E293B")
    )

    story = []

    # --- TITLE & HEADER BLOCK ---
    story.append(Paragraph("VIZUALX", title_style))
    story.append(Paragraph("On-Device Multimodal AI Perception & Intelligent Navigation for the Visually Impaired", subtitle_style))
    story.append(Paragraph("<b>Project Dossier & Whitepaper</b> | Architecture, Speech Gating, Phonetic Normalization & Multimodal AI", meta_style))
    story.append(HRFlowable(width="100%", thickness=1.5, color=c_accent, spaceBefore=0, spaceAfter=10))

    # --- EXECUTIVE SUMMARY ---
    story.append(Paragraph("Executive Summary", h1_style))
    story.append(Paragraph(
        "<b>VizualX</b> is an on-device, continuous multimodal AI perception assistant engineered for visually impaired users. "
        "Unlike conventional computer vision tools that act as raw sensor stream announcers, VizualX was designed around a fundamental human principle: "
        "<b>Audio bandwidth is cognitive bandwidth</b>. The application pairs high-speed local computer vision reflexes with an intelligent "
        "<b>Silence-by-Default AI Speech Arbiter</b>, a <b>Phonetic Speech Normalizer</b>, an on-demand <b>Assistive Reading Engine</b>, and a "
        "hybrid <b>Local Gemma 2B / Google Gemini 1.5 Flash Cloud Vision</b> intelligence stack.",
        body_style
    ))

    # Callout Box
    callout_data = [[
        Paragraph("<b>Core Engineering Rule (Silence is a Valid State):</b> "
                  "The system monitors 3D space continuously into internal memory, but strictly silences spoken output unless an immediate, "
                  "actionable safety hazard is detected or the user explicitly queries the environment.", callout_style)
    ]]
    callout_table = Table(callout_data, colWidths=[512])
    callout_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, -1), colors.HexColor("#EFF6FF")),
        ('BOX', (0, 0), (-1, -1), 1, c_accent),
        ('TOPPADDING', (0, 0), (-1, -1), 6),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 6),
        ('LEFTPADDING', (0, 0), (-1, -1), 10),
        ('RIGHTPADDING', (0, 0), (-1, -1), 10),
    ]))
    story.append(callout_table)
    story.append(Spacer(1, 6))

    # --- SECTION 1: THE REAL PROBLEM ---
    story.append(Paragraph("1. The Problem Space & The Flaws of Current Assistive Tech", h1_style))
    story.append(Paragraph(
        "Assistive mobility applications frequently fail in real-world situations because they are built as raw object detectors "
        "rather than actionable assistants. Testing revealed five critical pain points:",
        body_style
    ))

    story.append(Paragraph("• <b>Audio & Sensory Fatigue (The Chatterbox Failure):</b> Traditional tools speak every detected object in view (<i>'Bench on left... Tree ahead... Chair... Person... Sign'</i>). This creates severe cognitive overload and dangerously masks real environmental acoustic cues like traffic sounds, echoes, and footfalls.", bullet_style))
    story.append(Paragraph("• <b>Hallucinated Directions & Non-Actionable Speech:</b> Describing objects as 'on your left' or 'on your right' without millimeter head-pose tracking is inaccurate. Saying <i>'Caution, vehicle'</i> fails to tell a blind user what physical action to take (Stop? Step back? Proceed?).", bullet_style))
    story.append(Paragraph("• <b>Robotic 'Letter-by-Letter' TTS Spelling:</b> Stock Android Text-to-Speech engines treat ALL-CAPS words (e.g. <i>CAUTION, EXIT, STOP, ROOM 101</i>) as acronyms, spelling them out as <i>'C-A-U-T-I-O-N'</i> rather than pronouncing fluent spoken words.", bullet_style))
    story.append(Paragraph("• <b>Cloud Latency & Network Fragility:</b> Cloud-only vision APIs produce 2–5 second latency and blackout in subways, basements, and transit corridors where safety hazards require sub-100ms response times.", bullet_style))
    story.append(Paragraph("• <b>Visual UI Clutter:</b> Visual buttons and menus on mobile screens are inaccessible to blind users who need an intuitive, full-screen gesture surface.", bullet_style))
    
    story.append(Spacer(1, 6))

    # --- SECTION 2: THE SOLUTION ARCHITECTURE ---
    story.append(Paragraph("2. The Solution: What We Built Across VizualX", h1_style))
    story.append(Paragraph(
        "VizualX solves these challenges through a modular, decoupled multimodal architecture combining local on-device perception, "
        "strict speech arbiters, and generative visual reasoning:",
        body_style
    ))

    story.append(Paragraph("A. Silence-by-Default AI Speech Arbiter (ContextEngine)", h2_style))
    story.append(Paragraph(
        "Sensors monitor objects, text, and acoustic volume at 5 FPS into internal world state memory, but the <b>Speech Arbiter</b> enforces strict gating:",
        body_style
    ))
    story.append(Paragraph("• <b>Critical Safety Hazards (Spoken Immediately):</b> Approaching vehicles in the walking path, descending stairs/drop-offs, collision obstacles closing within 1.8m in the center trajectory, emergency sirens, and hazard warning signs (e.g. <i>'Caution Wet Floor'</i>).", bullet_style))
    story.append(Paragraph("• <b>Ambient Background Clutter (100% Silent):</b> Benches, trees, peripheral pedestrians walking past, and non-hazard signs (storefronts, room numbers) are tracked silently in memory without triggering audio speech.", bullet_style))
    story.append(Paragraph("• <b>Global Quiet Buffer:</b> A mandatory 5.0-second silence interval between routine informational messages ensures continuous quiet and situational awareness.", bullet_style))

    story.append(Paragraph("B. Phonetic Speech Normalizer & OCR Synthesizer (SpeechNormalizer)", h2_style))
    story.append(Paragraph(
        "To eliminate robotic letter-by-letter spelling, every spoken string is processed through a dedicated phonetic normalizer before audio synthesis:",
        body_style
    ))
    story.append(Paragraph("• <b>De-Acronymization:</b> Converts all-caps text (<i>CAUTION, RESTROOM, EXIT</i>) to sentence case so the TTS engine pronounces natural words while preserving genuine acronyms (<i>ATM, GPS, SOS, VIP</i>).", bullet_style))
    story.append(Paragraph("• <b>Spaced Character Collapsing:</b> Merges fragmented camera OCR characters (<i>'S T O P'</i> &rarr; <i>'Stop'</i>, <i>'E X I T'</i> &rarr; <i>'Exit'</i>).", bullet_style))
    story.append(Paragraph("• <b>Spatial & Building Abbreviation Expansion:</b> Automatically expands <i>'St.'</i> to <i>'Street'</i>, <i>'Rm.'</i> to <i>'Room'</i>, <i>'Bldg.'</i> to <i>'Building'</i>, <i>'No.'</i> to <i>'Number'</i>, and <i>'$12.50'</i> to <i>'12 dollars and 50 cents'</i>.", bullet_style))

    story.append(Paragraph("C. On-Demand Assistive Reading Tool (AssistiveReadingEngine)", h2_style))
    story.append(Paragraph(
        "A dedicated tool allowing visually impaired users to read documents, mail, food packaging, door plaques, and menus on demand:",
        body_style
    ))
    story.append(Paragraph("• <b>Spatial Text Flow:</b> ML Kit OCR text blocks are sorted top-to-bottom and left-to-right into coherent human reading paragraphs.", bullet_style))
    story.append(Paragraph("• <b>Dual Trigger Modes:</b> Triggered instantly by a single tap anywhere on the full-screen camera or via voice query (<i>'Read this'</i>, <i>'What does this document say?'</i>).", bullet_style))

    story.append(Paragraph("D. Hybrid Intelligence: Local Gemma 2B & Google Gemini 1.5 Flash", h2_style))
    story.append(Paragraph(
        "• <b>On-Device LLM Brain (LocalLlmBrain):</b> Uses quantized Gemma 2B / Phi-3 via MediaPipe LLM Inference to synthesize immediate active directive action commands under 8 words (<i>'Stop immediately, vehicle moving ahead'</i>, <i>'Obstacle close, sweep cane'</i>) without network connection.",
        body_style
    ))
    story.append(Paragraph(
        "• <b>Google Gemini 1.5 Flash Cloud Vision (GeminiApiClient):</b> Provides multimodal visual reasoning for complex queries (<i>'Describe what is in front of me'</i>, <i>'Is there an open seat?'</i>, <i>'What store is across the hallway?'</i>).",
        body_style
    ))

    story.append(Paragraph("E. Full-Screen Edge-to-Edge Gesture UX (MainScreen)", h2_style))
    story.append(Paragraph(
        "The user interface has zero buttons or visual clutter. The entire display is a 100% full-screen camera view with clean top tab pills for <b>Camera Feed</b> and <b>Google Maps Nav</b>. Blind accessibility is powered by universal touch gestures:",
        body_style
    ))
    story.append(Paragraph("• <b>Single Tap:</b> Triggers Assistive Reading on the current camera frame.", bullet_style))
    story.append(Paragraph("• <b>Double Tap:</b> Activates hands-free Voice AI inquiry.", bullet_style))
    story.append(Paragraph("• <b>Long Press:</b> Toggles Pause / Resume perception.", bullet_style))

    story.append(Spacer(1, 6))

    # --- SECTION 3: SYSTEM ARCHITECTURE & COMPARISON TABLE ---
    story.append(Paragraph("3. Technical Architecture & Capability Comparison", h1_style))
    
    # Table Comparison
    table_data = [
        [
            Paragraph("<b>Capability / Metric</b>", table_header_style),
            Paragraph("<b>Traditional Assistive Tools</b>", table_header_style),
            Paragraph("<b>VizualX Intelligent System</b>", table_header_style)
        ],
        [
            Paragraph("<b>Speech Gating Policy</b>", table_body_style),
            Paragraph("Continuous chatter; describes every object, tree, and bench.", table_body_style),
            Paragraph("<b>Silence is Valid</b>; speaks only for imminent safety hazards or user queries.", table_body_style)
        ],
        [
            Paragraph("<b>Sign & Text Handling</b>", table_body_style),
            Paragraph("Spells uppercase words letter-by-letter (C-A-U-T-I-O-N); OCR noise.", table_body_style),
            Paragraph("<b>Phonetic Speech Normalizer</b>; speaks fluent words; hazard signs alerted; ambient signs read on demand.", table_body_style)
        ],
        [
            Paragraph("<b>Hazard Latency</b>", table_body_style),
            Paragraph("1500ms – 4000ms (Cloud API roundtrip latency).", table_body_style),
            Paragraph("<b>&lt; 50ms</b> on-device reflex pipeline (MediaPipe + heuristic arbiter).", table_body_style)
        ],
        [
            Paragraph("<b>Navigation Output</b>", table_body_style),
            Paragraph("Vague spatial labels (<i>'Person on left'</i>).", table_body_style),
            Paragraph("<b>Direct Action Verbs</b> (<i>'Stop immediately'</i>, <i>'Sweep cane'</i>).", table_body_style)
        ],
        [
            Paragraph("<b>Generative Intelligence</b>", table_body_style),
            Paragraph("None or cloud-only text summary.", table_body_style),
            Paragraph("<b>Hybrid</b>: On-device Gemma 2B LLM + Google Gemini 1.5 Flash Multimodal Vision.", table_body_style)
        ],
        [
            Paragraph("<b>Interface Design</b>", table_body_style),
            Paragraph("Visual UI with buttons and small tap targets.", table_body_style),
            Paragraph("<b>100% Full-Screen Gesture UX</b> (Single tap, double tap, long press).", table_body_style)
        ]
    ]

    comp_table = Table(table_data, colWidths=[108, 194, 210])
    comp_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_primary),
        ('ALIGN', (0, 0), (-1, -1), 'LEFT'),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
        ('TOPPADDING', (0, 0), (-1, -1), 4),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 4),
        ('LEFTPADDING', (0, 0), (-1, -1), 5),
        ('RIGHTPADDING', (0, 0), (-1, -1), 5),
        ('GRID', (0, 0), (-1, -1), 0.5, c_border),
        ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, c_bg_light])
    ]))
    story.append(comp_table)
    story.append(Spacer(1, 6))

    # --- SECTION 4: VERIFICATION & TESTING MATRIX ---
    story.append(Paragraph("4. Verification & Testing Matrix", h1_style))
    story.append(Paragraph(
        "The codebase has been verified with <b>39 comprehensive automated unit tests</b> covering the entire pipeline:",
        body_style
    ))
    story.append(Paragraph("• <b>Context & Speech Arbiter Tests:</b> Verified critical vehicle warnings, stair drop-offs, side-object suppression, and hazard vs. ambient sign speech gating.", bullet_style))
    story.append(Paragraph("• <b>Phonetic Normalizer Tests:</b> Verified de-acronymization of all-caps signs, spaced letter merging (<i>S T O P</i> &rarr; <i>Stop</i>), abbreviation expansions, and OCR noise removal.", bullet_style))
    story.append(Paragraph("• <b>Assistive Reading Tests:</b> Verified spatial text sorting and OCR paragraph extraction lifecycle.", bullet_style))
    story.append(Paragraph("• <b>Local LLM Brain Tests:</b> Verified prompt templates, action command sanitization, and deterministic fallback behavior.", bullet_style))

    story.append(Spacer(1, 6))

    # --- SECTION 5: CONCLUSION & ROADMAP ---
    story.append(Paragraph("5. Future Roadmap & Smart Glasses Form Factor", h1_style))
    story.append(Paragraph(
        "Because VizualX decouples the perception and world state engines completely from the visual UI, the entire core intelligence stack "
        "is ready for deployment directly onto <b>Smart Glasses</b> equipped with front-facing camera frames and bone-conduction audio transducers. "
        "Next iterations will incorporate stereo depth sensors, ultrasonic rangefinders, and edge NPU acceleration for next-generation accessibility.",
        body_style
    ))

    doc.build(story, canvasmaker=NumberedCanvas)

if __name__ == "__main__":
    output_path = os.path.abspath("VizualX_Project_Report.pdf")
    build_pdf(output_path)
    print(f"PDF successfully generated at: {output_path}")

