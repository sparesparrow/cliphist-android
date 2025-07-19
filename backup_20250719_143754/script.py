import os
import json

# Create the complete Android project structure
project_structure = {
    "app": {
        "src": {
            "main": {
                "java": {
                    "com": {
                        "clipboardhistory": {
                            "presentation": {
                                "ui": {
                                    "theme": {},
                                    "components": {},
                                    "screens": {}
                                },
                                "viewmodels": {},
                                "services": {}
                            },
                            "domain": {
                                "model": {},
                                "repository": {},
                                "usecase": {}
                            },
                            "data": {
                                "database": {},
                                "repository": {},
                                "encryption": {}
                            },
                            "di": {},
                            "utils": {}
                        }
                    }
                },
                "res": {
                    "layout": {},
                    "values": {},
                    "drawable": {},
                    "xml": {}
                }
            },
            "test": {
                "java": {
                    "com": {
                        "clipboardhistory": {}
                    }
                }
            },
            "androidTest": {
                "java": {
                    "com": {
                        "clipboardhistory": {}
                    }
                }
            }
        }
    },
    ".github": {
        "workflows": {}
    },
    "docs": {}
}

def create_directory_structure(structure, base_path=""):
    """Create directory structure recursively"""
    for name, content in structure.items():
        current_path = os.path.join(base_path, name)
        os.makedirs(current_path, exist_ok=True)
        if isinstance(content, dict):
            create_directory_structure(content, current_path)

# Create the directory structure
create_directory_structure(project_structure)

print("Android project structure created successfully!")
print("\nProject structure:")
for root, dirs, files in os.walk("."):
    level = root.replace(".", "").count(os.sep)
    indent = " " * 2 * level
    print(f"{indent}{os.path.basename(root)}/")
    subindent = " " * 2 * (level + 1)
    for file in files:
        if not file.startswith('.'):
            print(f"{subindent}{file}")