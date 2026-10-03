import bz2
import xml.etree.ElementTree as ET
from pathlib import Path
import mwparserfromhell
import json

DUMP_PATH = Path(
    "../data/raw/"
    "enwiki-20261001-pages-articles-multistream1.xml-p1p41242.bz2"
)

MAX_PAGES = 20

# MediaWiki XML namespace from the dump's <mediawiki> root element.
MW_NS = "{http://www.mediawiki.org/xml/export-0.11/}"

OUTPUT_PATH = Path("../data/processed/wikipedia-dev-10k.jsonl")
SOURCE_NAME = "enwiki-20261001"
CORPUS_SIZE = 10_000

def inspect_dump(dump_path: Path) -> None:
    pages_seen = 0

    with bz2.open(dump_path, mode="rb") as dump_file:
        context = ET.iterparse(dump_file, events=("end",))

        for _, elem in context:
            if elem.tag != f"{MW_NS}page":
                continue

            title = elem.findtext(f"{MW_NS}title")
            namespace = elem.findtext(f"{MW_NS}ns")
            page_id = elem.findtext(f"{MW_NS}id")

            redirect = elem.find(f"{MW_NS}redirect") is not None

            revision = elem.find(f"{MW_NS}revision")
            text = None

            if revision is not None:
                text = revision.findtext(f"{MW_NS}text")

            text_length = len(text) if text else 0
            accepted = is_valid_document(
                namespace=namespace,
                redirect=redirect,
                text=text,
            )
            
            print(
                f"id={page_id:<8} "
                f"ns={namespace:<3} "
                f"redirect={str(redirect):<5} "
                f"accepted={str(accepted):<5} "
                f"text_length={text_length:<8} "
                f"title={title}"
            )   

            pages_seen += 1

            # Release the completed <page> subtree from memory.
            elem.clear()

            if pages_seen >= MAX_PAGES:
                break

def count_documents(dump_path: Path) -> None:
    total = 0
    accepted = 0
    redirects = 0
    non_main_namespace = 0
    empty_text = 0

    with bz2.open(dump_path, mode="rb") as dump_file:
        context = ET.iterparse(dump_file, events=("end",))

        for _, elem in context:
            if elem.tag != f"{MW_NS}page":
                continue

            total += 1

            namespace = elem.findtext(f"{MW_NS}ns")
            redirect = elem.find(f"{MW_NS}redirect") is not None

            revision = elem.find(f"{MW_NS}revision")
            text = (
                revision.findtext(f"{MW_NS}text")
                if revision is not None
                else None
            )

            if namespace != "0":
                non_main_namespace += 1
            elif redirect:
                redirects += 1
            elif text is None or not text.strip():
                empty_text += 1
            else:
                accepted += 1

            elem.clear()

    print()
    print("Corpus scan")
    print("-----------")
    print(f"Total pages:        {total:,}")
    print(f"Accepted documents: {accepted:,}")
    print(f"Redirects:          {redirects:,}")
    print(f"Non-main namespace: {non_main_namespace:,}")
    print(f"Empty text:         {empty_text:,}")

def is_valid_document(
    namespace: str | None,
    redirect: bool,
    text: str | None,
) -> bool:
    """Return True when a Wikipedia page should become a ShardSearch document."""

    if namespace != "0":
        return False

    if redirect:
        return False

    if text is None or not text.strip():
        return False

    return True

def clean_wikitext(text: str) -> str:
    """Convert MediaWiki wikitext into plain searchable text."""

    wikicode = mwparserfromhell.parse(text)

    cleaned = wikicode.strip_code(
        normalize=True,
        collapse=True,
    )

    return cleaned.strip()

def inspect_cleaning(dump_path: Path) -> None:
    with bz2.open(dump_path, mode="rb") as dump_file:
        context = ET.iterparse(dump_file, events=("end",))

        for _, elem in context:
            if elem.tag != f"{MW_NS}page":
                continue

            title = elem.findtext(f"{MW_NS}title")
            namespace = elem.findtext(f"{MW_NS}ns")
            redirect = elem.find(f"{MW_NS}redirect") is not None

            revision = elem.find(f"{MW_NS}revision")
            text = (
                revision.findtext(f"{MW_NS}text")
                if revision is not None
                else None
            )

            if is_valid_document(namespace, redirect, text):
                cleaned = clean_wikitext(text)

                print(f"TITLE: {title}")
                print(f"RAW LENGTH: {len(text):,}")
                print(f"CLEAN LENGTH: {len(cleaned):,}")
                print()
                print("FIRST 2000 CLEAN CHARACTERS")
                print("---------------------------")
                print(cleaned[:2000])

                return

            elem.clear()

def extract_corpus(
    dump_path: Path,
    output_path: Path,
    limit: int,
) -> None:
    accepted = 0
    pages_seen = 0

    output_path.parent.mkdir(parents=True, exist_ok=True)

    with (
        bz2.open(dump_path, mode="rb") as dump_file,
        output_path.open("w", encoding="utf-8") as output_file,
    ):
        context = ET.iterparse(dump_file, events=("end",))

        for _, elem in context:
            if elem.tag != f"{MW_NS}page":
                continue

            pages_seen += 1

            title = elem.findtext(f"{MW_NS}title")
            namespace = elem.findtext(f"{MW_NS}ns")
            page_id = elem.findtext(f"{MW_NS}id")
            redirect = elem.find(f"{MW_NS}redirect") is not None

            revision = elem.find(f"{MW_NS}revision")
            text = (
                revision.findtext(f"{MW_NS}text")
                if revision is not None
                else None
            )

            if is_valid_document(namespace, redirect, text):
                cleaned_text = clean_wikitext(text)

                # Cleaning can theoretically remove all meaningful content.
                if cleaned_text.strip():
                    document = {
                        "id": int(page_id),
                        "title": title,
                        "text": cleaned_text,
                        "source": SOURCE_NAME,
                    }

                    output_file.write(
                        json.dumps(
                            document,
                            ensure_ascii=False,
                            separators=(",", ":"),
                        )
                        + "\n"
                    )

                    accepted += 1

                    if accepted % 1000 == 0:
                        print(
                            f"Extracted {accepted:,}/{limit:,} documents "
                            f"after scanning {pages_seen:,} pages"
                        )

                    if accepted >= limit:
                        elem.clear()
                        break

            elem.clear()

    print()
    print("Extraction complete")
    print("-------------------")
    print(f"Pages scanned:       {pages_seen:,}")
    print(f"Documents extracted: {accepted:,}")
    print(f"Output:              {output_path}")


if __name__ == "__main__":
    extract_corpus(
        dump_path=DUMP_PATH,
        output_path=OUTPUT_PATH,
        limit=CORPUS_SIZE,
    )



"""
###Final Version###

import bz2
import json
import xml.etree.ElementTree as ET
from pathlib import Path

import mwparserfromhell


DUMP_PATH = Path(
    "../data/raw/"
    "enwiki-20261001-pages-articles-multistream1.xml-p1p41242.bz2"
)

OUTPUT_PATH = Path(
    "../data/processed/wikipedia-dev-10k.jsonl"
)

SOURCE_NAME = "enwiki-20261001"
CORPUS_SIZE = 10_000

MW_NS = "{http://www.mediawiki.org/xml/export-0.11/}"


def is_valid_document(
    namespace: str | None,
    redirect: bool,
    text: str | None,
) -> bool:
    #Return True if a Wikipedia page should become a ShardSearch document.

    return (
        namespace == "0"
        and not redirect
        and text is not None
        and bool(text.strip())
    )


def clean_wikitext(text: str) -> str:
    ###Convert MediaWiki wikitext into plain searchable text.

    wikicode = mwparserfromhell.parse(text)

    return wikicode.strip_code(
        normalize=True,
        collapse=True,
    ).strip()


def extract_corpus(
    dump_path: Path,
    output_path: Path,
    limit: int,
) -> None:
    pages_seen = 0
    documents_written = 0

    output_path.parent.mkdir(
        parents=True,
        exist_ok=True,
    )

    with (
        bz2.open(dump_path, mode="rb") as dump_file,
        output_path.open("w", encoding="utf-8") as output_file,
    ):
        context = ET.iterparse(
            dump_file,
            events=("end",),
        )

        for _, elem in context:
            if elem.tag != f"{MW_NS}page":
                continue

            pages_seen += 1

            title = elem.findtext(f"{MW_NS}title")
            namespace = elem.findtext(f"{MW_NS}ns")
            page_id = elem.findtext(f"{MW_NS}id")

            redirect = (
                elem.find(f"{MW_NS}redirect")
                is not None
            )

            revision = elem.find(f"{MW_NS}revision")

            text = (
                revision.findtext(f"{MW_NS}text")
                if revision is not None
                else None
            )

            if is_valid_document(
                namespace=namespace,
                redirect=redirect,
                text=text,
            ):
                cleaned_text = clean_wikitext(text)

                if cleaned_text:
                    document = {
                        "id": int(page_id),
                        "title": title,
                        "text": cleaned_text,
                        "source": SOURCE_NAME,
                    }

                    output_file.write(
                        json.dumps(
                            document,
                            ensure_ascii=False,
                            separators=(",", ":"),
                        )
                        + "\n"
                    )

                    documents_written += 1

                    if documents_written % 1000 == 0:
                        print(
                            f"Extracted "
                            f"{documents_written:,}/{limit:,} documents "
                            f"after scanning {pages_seen:,} pages"
                        )

                    if documents_written >= limit:
                        elem.clear()
                        break

            elem.clear()

    print()
    print("Extraction complete")
    print("-------------------")
    print(f"Pages scanned:       {pages_seen:,}")
    print(f"Documents extracted: {documents_written:,}")
    print(f"Output:              {output_path}")


if __name__ == "__main__":
    extract_corpus(
        dump_path=DUMP_PATH,
        output_path=OUTPUT_PATH,
        limit=CORPUS_SIZE,
    )
"""