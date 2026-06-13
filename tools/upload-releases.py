import os
import sys
import subprocess
import requests
from urllib.parse import quote

# 1. Retrieve github credentials from git-credential-manager
def get_github_token():
    print("Querying Git Credential Manager for github.com credentials...")
    input_data = "protocol=https\nhost=github.com\nusername=evenex161\n\n"
    try:
        proc = subprocess.Popen(
            ["git", "credential", "fill"],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )
        stdout, stderr = proc.communicate(input=input_data)
        if proc.returncode != 0:
            print(f"Error querying git credential: {stderr}", file=sys.stderr)
            return None
        
        for line in stdout.splitlines():
            if line.startswith("password="):
                token = line.split("=", 1)[1].strip()
                if token:
                    print("Successfully retrieved GitHub token from Git Credential Manager.")
                    return token
    except Exception as e:
        print(f"Exception while running git credential: {e}", file=sys.stderr)
    
    return None

def main():
    token = get_github_token()
    if not token:
        print("Failed to retrieve GitHub token. Cannot proceed with release upload.", file=sys.stderr)
        sys.exit(1)
        
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    releases_dir = os.path.join(base_dir, "releases")
    
    if not os.path.isdir(releases_dir):
        print(f"Releases directory not found at: {releases_dir}", file=sys.stderr)
        sys.exit(1)
        
    headers = {
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28"
    }
    
    # Scan subdirectories under releases/
    version_dirs = []
    for entry in os.scandir(releases_dir):
        if entry.is_dir() and entry.name.startswith("mc-"):
            version_dirs.append(entry)
            
    if not version_dirs:
        print("No Minecraft version directories (starting with 'mc-') found in releases/.")
        sys.exit(0)
        
    print(f"Found {len(version_dirs)} version directories to release: {[d.name for d in version_dirs]}")
    
    repo = "evenex161/servermanagementplus"
    
    for vdir in version_dirs:
        version = vdir.name.replace("mc-", "", 1)
        tag_name = f"v2.1.0-mc{version}"
        release_name = f"ServerManagement+ v2.1.0 - Minecraft {version}"
        target_branch = f"mc/{version}"
        
        print(f"\n==================== Processing Release: {release_name} (Tag: {tag_name}) ====================")
        
        # Read changelog if exists
        changelog_path = os.path.join(vdir.path, "CHANGELOG_v2.1.0.md")
        body_content = ""
        if os.path.exists(changelog_path):
            try:
                with open(changelog_path, "r", encoding="utf-8") as f:
                    body_content = f.read()
                print(f"Loaded changelog from: {changelog_path}")
            except Exception as e:
                print(f"Warning: Failed to read changelog file: {e}")
        
        if not body_content:
            body_content = f"Release build for ServerManagement+ v2.1.0 on Minecraft {version}."
            
        # Find all JAR assets in the version directory
        assets = []
        for file_entry in os.scandir(vdir.path):
            if file_entry.is_file() and file_entry.name.endswith(".jar"):
                assets.append(file_entry.path)
                
        if not assets:
            print(f"No JAR assets found in {vdir.path}. Skipping release creation.")
            continue
            
        print(f"Found {len(assets)} JAR assets to upload.")
        
        # Check if release already exists
        check_url = f"https://api.github.com/repos/{repo}/releases/tags/{tag_name}"
        r = requests.get(check_url, headers=headers)
        
        release_id = None
        if r.status_code == 200:
            existing_release = r.json()
            release_id = existing_release["id"]
            print(f"Release already exists for tag {tag_name} (ID: {release_id}). Re-creating to upload fresh assets...")
            # Delete existing release
            del_url = f"https://api.github.com/repos/{repo}/releases/{release_id}"
            del_r = requests.delete(del_url, headers=headers)
            if del_r.status_code == 204:
                print("Deleted existing release successfully.")
            else:
                print(f"Failed to delete existing release: {del_r.status_code} {del_r.text}", file=sys.stderr)
                # Try to delete tag as well just in case
            
            # Let's delete the git tag reference so github lets us create it fresh on the correct commitish
            del_tag_url = f"https://api.github.com/repos/{repo}/git/refs/tags/{tag_name}"
            requests.delete(del_tag_url, headers=headers)
            
            release_id = None
            
        # Create Release
        create_url = f"https://api.github.com/repos/{repo}/releases"
        payload = {
            "tag_name": tag_name,
            "target_commitish": target_branch,
            "name": release_name,
            "body": body_content,
            "draft": False,
            "prerelease": False
        }
        
        print(f"Creating release on GitHub targeting branch {target_branch}...")
        cr = requests.post(create_url, headers=headers, json=payload)
        if cr.status_code not in (200, 201):
            print(f"Failed to create release: {cr.status_code} {cr.text}", file=sys.stderr)
            continue
            
        release_data = cr.json()
        release_id = release_data["id"]
        upload_url_template = release_data["upload_url"] # e.g. "https://uploads.github.com/.../assets{?name,label}"
        
        print(f"Release created successfully! ID: {release_id}")
        
        # Upload each asset
        # The upload_url has query parameter placeholders like {?name,label}
        # We need to format the url appropriately
        base_upload_url = upload_url_template.split("{")[0]
        
        for asset_path in assets:
            asset_name = os.path.basename(asset_path)
            print(f"  Uploading asset: {asset_name} ...")
            
            upload_url = f"{base_upload_url}?name={quote(asset_name)}"
            
            upload_headers = headers.copy()
            upload_headers["Content-Type"] = "application/octet-stream"
            
            try:
                with open(asset_path, "rb") as f:
                    file_data = f.read()
                
                ur = requests.post(upload_url, headers=upload_headers, data=file_data)
                if ur.status_code in (200, 201):
                    print(f"    Asset uploaded successfully.")
                else:
                    print(f"    Failed to upload asset: {ur.status_code} {ur.text}", file=sys.stderr)
            except Exception as e:
                print(f"    Error reading/uploading file {asset_name}: {e}", file=sys.stderr)
                
    print("\nAll releases and assets processed successfully!")

if __name__ == "__main__":
    main()
