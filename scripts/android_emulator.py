#!/usr/bin/env python3
"""
AndroidEmulator Interface
Unified interface for Android emulator control with action-specific responses.
"""

import subprocess
import json
import argparse
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from typing import List, Optional, Dict, Any
from datetime import datetime
import os
import re

# Result Types
@dataclass
class UIElement:
    """Represents a UI element with key properties"""
    text: str
    bounds: str
    content_desc: str
    resource_id: str
    class_name: str
    clickable: bool
    focused: bool
    x: int = 0
    y: int = 0
    width: int = 0
    height: int = 0

@dataclass
class BaseResult:
    """Base result class for all operations"""
    success: bool
    timestamp: str
    errors: List[str] = field(default_factory=list)
    warnings: List[str] = field(default_factory=list)

@dataclass
class ScreenshotResult(BaseResult):
    """Result from screenshot operation"""
    screenshot_path: str = ""
    ui_elements: List[UIElement] = field(default_factory=list)
    current_screen: str = "unknown"
    available_actions: List[str] = field(default_factory=list)
    input_fields: List[UIElement] = field(default_factory=list)
    buttons: List[UIElement] = field(default_factory=list)

@dataclass
class InteractionResult(BaseResult):
    """Result from UI interaction (tap, input, etc.)"""
    action_performed: str = ""
    target_element: Optional[UIElement] = None
    ui_state_changed: bool = False
    new_screen_state: str = ""
    follow_up_suggestions: List[str] = field(default_factory=list)

@dataclass
class UIStateResult(BaseResult):
    """Result from UI state analysis"""
    current_screen: str = "unknown"
    focused_element: Optional[UIElement] = None
    interactive_elements: List[UIElement] = field(default_factory=list)
    text_content: List[str] = field(default_factory=list)
    input_fields: List[UIElement] = field(default_factory=list)

@dataclass
class MessageResult(BaseResult):
    """Result from message sending operation"""
    message_sent: bool = False
    message_content: str = ""
    chat_state: str = "unknown"
    backend_response: Optional[str] = None
    conversation_state: str = "unknown"

class AndroidEmulator:
    """Unified Android emulator interface"""
    
    def __init__(self, device_id: str = "emulator-5554", project_root: str = None):
        self.device_id = device_id
        # Auto-detect project root if not provided
        if project_root is None:
            script_dir = os.path.dirname(os.path.abspath(__file__))
            self.project_root = os.path.dirname(script_dir)
        else:
            self.project_root = project_root
        self.scripts_dir = os.path.join(self.project_root, "scripts")
        self.logs_dir = os.path.join(self.project_root, "logs")
    
    def _run_command(self, command: List[str], capture_output: bool = True) -> subprocess.CompletedProcess:
        """Run shell command and return result"""
        try:
            result = subprocess.run(
                command, 
                capture_output=capture_output, 
                text=True, 
                cwd=self.project_root,
                timeout=30
            )
            return result
        except subprocess.TimeoutExpired:
            return subprocess.CompletedProcess(command, 1, "", "Command timed out")
        except Exception as e:
            return subprocess.CompletedProcess(command, 1, "", str(e))
    
    def _get_timestamp(self) -> str:
        """Get current timestamp"""
        return datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    def _parse_ui_dump(self, xml_content: str) -> List[UIElement]:
        """Parse UI dump XML and extract relevant elements"""
        elements = []
        try:
            root = ET.fromstring(xml_content)
            for node in root.iter('node'):
                text = node.get('text', '')
                content_desc = node.get('content-desc', '')
                bounds = node.get('bounds', '')
                resource_id = node.get('resource-id', '')
                class_name = node.get('class', '')
                clickable = node.get('clickable', 'false') == 'true'
                focused = node.get('focused', 'false') == 'true'
                
                # Skip empty or irrelevant elements
                if not text and not content_desc and not clickable:
                    continue
                
                # Parse bounds to get coordinates
                x, y, width, height = 0, 0, 0, 0
                if bounds:
                    match = re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', bounds)
                    if match:
                        x1, y1, x2, y2 = map(int, match.groups())
                        x, y = (x1 + x2) // 2, (y1 + y2) // 2
                        width, height = x2 - x1, y2 - y1
                
                element = UIElement(
                    text=text,
                    bounds=bounds,
                    content_desc=content_desc,
                    resource_id=resource_id,
                    class_name=class_name,
                    clickable=clickable,
                    focused=focused,
                    x=x,
                    y=y,
                    width=width,
                    height=height
                )
                elements.append(element)
        except Exception as e:
            # Return empty list if parsing fails
            pass
        
        return elements
    
    def _analyze_screen_state(self, elements: List[UIElement]) -> str:
        """Analyze UI elements to determine current screen state"""
        # Look for key indicators
        texts = [elem.text.lower() for elem in elements if elem.text]
        content_descs = [elem.content_desc.lower() for elem in elements if elem.content_desc]
        all_text = texts + content_descs
        
        if any('login' in text or 'sign in' in text for text in all_text):
            return "login"
        elif any('home' in text for text in all_text):
            return "home"
        elif any('chat' in text or 'message' in text for text in all_text):
            return "chat"
        elif any('settings' in text for text in all_text):
            return "settings"
        else:
            return "unknown"
    
    def _get_available_actions(self, elements: List[UIElement], screen_state: str) -> List[str]:
        """Determine available actions based on current state"""
        actions = []
        
        # Check for clickable elements
        clickable_elements = [elem for elem in elements if elem.clickable]
        
        if any(elem.content_desc == "Send message" for elem in clickable_elements):
            actions.append("send_message")
        
        if any("login" in elem.text.lower() or "sign in" in elem.text.lower() for elem in clickable_elements):
            actions.append("login")
        
        if any("menu" in elem.content_desc.lower() for elem in clickable_elements):
            actions.append("open_menu")
        
        # Check for input fields
        input_fields = [elem for elem in elements if 'EditText' in elem.class_name]
        if input_fields:
            actions.append("input_text")
        
        # Always available
        actions.extend(["screenshot", "tap", "get_ui_state"])
        
        return actions
    
    def screenshot(self) -> ScreenshotResult:
        """Take screenshot and analyze UI state"""
        result = ScreenshotResult(
            success=False,
            timestamp=self._get_timestamp()
        )
        
        try:
            # Take screenshot
            screenshot_cmd = [os.path.join(self.scripts_dir, "screenshot.sh")]
            screenshot_result = self._run_command(screenshot_cmd)
            
            if screenshot_result.returncode != 0:
                result.errors.append(f"Screenshot failed: {screenshot_result.stderr}")
                return result
            
            # Get UI dump
            ui_dump_cmd = [os.path.join(self.scripts_dir, "interact.sh"), "ui-dump"]
            ui_result = self._run_command(ui_dump_cmd)
            
            if ui_result.returncode != 0:
                result.errors.append(f"UI dump failed: {ui_result.stderr}")
                return result
            
            # Parse UI elements
            ui_dump_path = os.path.join(self.logs_dir, "ui_dump.xml")
            if os.path.exists(ui_dump_path):
                with open(ui_dump_path, 'r') as f:
                    xml_content = f.read()
                elements = self._parse_ui_dump(xml_content)
            else:
                elements = []
                result.warnings.append("UI dump file not found")
            
            # Find latest screenshot
            screenshots_dir = os.path.join(self.logs_dir, "screenshots")
            if os.path.exists(screenshots_dir):
                screenshots = [f for f in os.listdir(screenshots_dir) if f.endswith('.png')]
                if screenshots:
                    latest_screenshot = max(screenshots, key=lambda f: os.path.getctime(os.path.join(screenshots_dir, f)))
                    result.screenshot_path = os.path.join(screenshots_dir, latest_screenshot)
            
            # Analyze state
            result.current_screen = self._analyze_screen_state(elements)
            result.available_actions = self._get_available_actions(elements, result.current_screen)
            
            # Categorize elements
            result.ui_elements = elements
            result.input_fields = [elem for elem in elements if 'EditText' in elem.class_name]
            result.buttons = [elem for elem in elements if elem.clickable and ('Button' in elem.class_name or elem.content_desc)]
            
            result.success = True
            
        except Exception as e:
            result.errors.append(f"Screenshot operation failed: {str(e)}")
        
        return result
    
    def tap(self, x: int, y: int) -> InteractionResult:
        """Tap at specific coordinates"""
        result = InteractionResult(
            success=False,
            timestamp=self._get_timestamp(),
            action_performed=f"tap({x}, {y})"
        )
        
        try:
            # Get initial state
            initial_screenshot = self.screenshot()
            initial_state = initial_screenshot.current_screen
            
            # Perform tap
            tap_cmd = [os.path.join(self.scripts_dir, "interact.sh"), "tap", str(x), str(y)]
            tap_result = self._run_command(tap_cmd)
            
            if tap_result.returncode != 0:
                result.errors.append(f"Tap failed: {tap_result.stderr}")
                return result
            
            # Get new state
            import time
            time.sleep(1)  # Brief pause for UI to update
            new_screenshot = self.screenshot()
            new_state = new_screenshot.current_screen
            
            result.ui_state_changed = (initial_state != new_state)
            result.new_screen_state = new_state
            
            # Find what was tapped
            for element in initial_screenshot.ui_elements:
                if (element.clickable and 
                    abs(element.x - x) < element.width // 2 and 
                    abs(element.y - y) < element.height // 2):
                    result.target_element = element
                    break
            
            # Suggest follow-up actions
            result.follow_up_suggestions = new_screenshot.available_actions
            
            result.success = True
            
        except Exception as e:
            result.errors.append(f"Tap operation failed: {str(e)}")
        
        return result
    
    def tap_element(self, selector: str) -> InteractionResult:
        """Tap element by text or content description"""
        result = InteractionResult(
            success=False,
            timestamp=self._get_timestamp(),
            action_performed=f"tap_element('{selector}')"
        )
        
        try:
            # Get current UI state
            screenshot_result = self.screenshot()
            if not screenshot_result.success:
                result.errors.extend(screenshot_result.errors)
                return result
            
            # Find element - first try direct clickable match
            target_element = None
            for element in screenshot_result.ui_elements:
                if (element.clickable and 
                    (selector.lower() in element.text.lower() or 
                     selector.lower() in element.content_desc.lower())):
                    target_element = element
                    break
            
            # If not found, try to find by content-desc and look for clickable parent
            if not target_element:
                for element in screenshot_result.ui_elements:
                    if (selector.lower() in element.content_desc.lower()):
                        # Try to find clickable parent by using the coordinates of this element
                        # Look for clickable elements that contain this element's coordinates
                        for clickable_elem in screenshot_result.ui_elements:
                            if (clickable_elem.clickable and 
                                element.bounds and clickable_elem.bounds):
                                # Parse bounds to check if clickable element contains the target
                                target_match = re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', element.bounds)
                                clickable_match = re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', clickable_elem.bounds)
                                if target_match and clickable_match:
                                    tx1, ty1, tx2, ty2 = map(int, target_match.groups())
                                    cx1, cy1, cx2, cy2 = map(int, clickable_match.groups())
                                    # Check if target is within clickable bounds
                                    if (cx1 <= tx1 <= cx2 and cy1 <= ty1 <= cy2 and
                                        cx1 <= tx2 <= cx2 and cy1 <= ty2 <= cy2):
                                        target_element = clickable_elem
                                        break
                        if target_element:
                            break
            
            if not target_element:
                result.errors.append(f"Element '{selector}' not found or not clickable")
                return result
            
            # Tap the element
            tap_result = self.tap(target_element.x, target_element.y)
            result.success = tap_result.success
            result.errors.extend(tap_result.errors)
            result.target_element = target_element
            result.ui_state_changed = tap_result.ui_state_changed
            result.new_screen_state = tap_result.new_screen_state
            result.follow_up_suggestions = tap_result.follow_up_suggestions
            
        except Exception as e:
            result.errors.append(f"Tap element operation failed: {str(e)}")
        
        return result
    
    def input_text(self, text: str) -> InteractionResult:
        """Input text into focused field"""
        result = InteractionResult(
            success=False,
            timestamp=self._get_timestamp(),
            action_performed=f"input_text('{text}')"
        )
        
        try:
            # Input text
            input_cmd = [os.path.join(self.scripts_dir, "interact.sh"), "text", text]
            input_result = self._run_command(input_cmd)
            
            if input_result.returncode != 0:
                result.errors.append(f"Text input failed: {input_result.stderr}")
                return result
            
            # Get updated state
            import time
            time.sleep(0.5)  # Brief pause for text to register
            new_screenshot = self.screenshot()
            
            result.new_screen_state = new_screenshot.current_screen
            result.follow_up_suggestions = new_screenshot.available_actions
            result.success = True
            
        except Exception as e:
            result.errors.append(f"Text input operation failed: {str(e)}")
        
        return result
    
    def send_message(self, message: str) -> MessageResult:
        """Send a message in chat interface"""
        result = MessageResult(
            success=False,
            timestamp=self._get_timestamp(),
            message_content=message
        )
        
        try:
            # Input the message
            input_result = self.input_text(message)
            if not input_result.success:
                result.errors.extend(input_result.errors)
                return result
            
            # Tap send button
            send_result = self.tap_element("Send message")
            if not send_result.success:
                result.errors.extend(send_result.errors)
                return result
            
            result.message_sent = True
            result.chat_state = "message_sent"
            result.success = True
            
        except Exception as e:
            result.errors.append(f"Send message operation failed: {str(e)}")
        
        return result
    
    def get_ui_state(self) -> UIStateResult:
        """Get detailed UI state information"""
        result = UIStateResult(
            success=False,
            timestamp=self._get_timestamp()
        )
        
        try:
            screenshot_result = self.screenshot()
            if not screenshot_result.success:
                result.errors.extend(screenshot_result.errors)
                return result
            
            result.current_screen = screenshot_result.current_screen
            result.interactive_elements = [elem for elem in screenshot_result.ui_elements if elem.clickable]
            result.input_fields = screenshot_result.input_fields
            result.text_content = [elem.text for elem in screenshot_result.ui_elements if elem.text]
            
            # Find focused element
            for element in screenshot_result.ui_elements:
                if element.focused:
                    result.focused_element = element
                    break
            
            result.success = True
            
        except Exception as e:
            result.errors.append(f"Get UI state operation failed: {str(e)}")
        
        return result

def main():
    """CLI interface for AndroidEmulator"""
    parser = argparse.ArgumentParser(description="Android Emulator Interface")
    parser.add_argument("action", choices=[
        "screenshot", "tap", "tap-element", "input-text", "send-message", "ui-state"
    ], help="Action to perform")
    parser.add_argument("--x", type=int, help="X coordinate for tap")
    parser.add_argument("--y", type=int, help="Y coordinate for tap")
    parser.add_argument("--text", help="Text to input or element to find")
    parser.add_argument("--device", default="emulator-5554", help="Device ID")
    parser.add_argument("--json", action="store_true", help="Output as JSON")
    
    args = parser.parse_args()
    
    emulator = AndroidEmulator(device_id=args.device)
    result = None
    
    if args.action == "screenshot":
        result = emulator.screenshot()
    elif args.action == "tap":
        if args.x is None or args.y is None:
            print("Error: --x and --y are required for tap action")
            return 1
        result = emulator.tap(args.x, args.y)
    elif args.action == "tap-element":
        if not args.text:
            print("Error: --text is required for tap-element action")
            return 1
        result = emulator.tap_element(args.text)
    elif args.action == "input-text":
        if not args.text:
            print("Error: --text is required for input-text action")
            return 1
        result = emulator.input_text(args.text)
    elif args.action == "send-message":
        if not args.text:
            print("Error: --text is required for send-message action")
            return 1
        result = emulator.send_message(args.text)
    elif args.action == "ui-state":
        result = emulator.get_ui_state()
    
    if result:
        if args.json:
            # Convert dataclass to dict for JSON serialization
            import dataclasses
            result_dict = dataclasses.asdict(result)
            print(json.dumps(result_dict, indent=2))
        else:
            print(f"Success: {result.success}")
            if result.errors:
                print(f"Errors: {result.errors}")
            if hasattr(result, 'current_screen'):
                print(f"Screen: {result.current_screen}")
            if hasattr(result, 'available_actions'):
                print(f"Available actions: {result.available_actions}")
    
    return 0 if result and result.success else 1

if __name__ == "__main__":
    exit(main())