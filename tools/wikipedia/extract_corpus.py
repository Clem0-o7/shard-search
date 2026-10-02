import bz2
import xml.etree.ElementTree as ET
from pathlib import Path


DUMP_PATH = Path(
    "../data/raw/"
    "enwiki-20261001-pages-articles-multistream1.xml-p1p41242.bz2"
)

MAX_PAGES = 20

# MediaWiki XML namespace from the dump's <mediawiki> root element.
MW_NS = "{http://www.mediawiki.org/xml/export-0.11/}"


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

            print(
                f"id={page_id:<8} "
                f"ns={namespace:<3} "
                f"redirect={str(redirect):<5} "
                f"text_length={text_length:<8} "
                f"title={title}"
            )

            pages_seen += 1

            # Release the completed <page> subtree from memory.
            elem.clear()

            if pages_seen >= MAX_PAGES:
                break


if __name__ == "__main__":
    inspect_dump(DUMP_PATH)