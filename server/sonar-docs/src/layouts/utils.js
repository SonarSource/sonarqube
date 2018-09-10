import { sortBy } from 'lodash';

export function sortNodes(nodes) {
  return nodes.sort((a, b) => {
    if (a.frontmatter.order) {
      return b.frontmatter.order ? a.frontmatter.order - b.frontmatter.order : 1;
    }
    return a.frontmatter.title < b.frontmatter.title ? -1 : 1;
  });
}

const WORDS = 6;

function cutLeadingWords(str) {
  let words = 0;
  for (let i = str.length - 1; i >= 0; i--) {
    if (/\s/.test(str[i])) {
      words++;
    }
    if (words === WORDS) {
      return i > 0 ? `...${str.substring(i + 1)}` : str;
    }
  }
  return str;
}

function cutTrailingWords(str) {
  let words = 0;
  for (let i = 0; i < str.length; i++) {
    if (/\s/.test(str[i])) {
      words++;
    }
    if (words === WORDS) {
      return i < str.length - 1 ? `${str.substring(0, i)}...` : str;
    }
  }
  return str;
}

export function cutWords(tokens) {
  const result = [];
  let length = 0;

  const highlightPos = tokens.findIndex(token => token.marked);
  if (highlightPos > 0) {
    const text = cutLeadingWords(tokens[highlightPos - 1].text);
    result.push({ text, marked: false });
    length += text.length;
  }

  result.push(tokens[highlightPos]);
  length += tokens[highlightPos].text.length;

  for (let i = highlightPos + 1; i < tokens.length; i++) {
    if (length + tokens[i].text.length > 100) {
      const text = cutTrailingWords(tokens[i].text);
      result.push({ text, marked: false });
      return result;
    } else {
      result.push(tokens[i]);
      length += tokens[i].text.length;
    }
  }

  return result;
}

export function highlightMarks(str, marks) {
  const sortedMarks = sortBy(
    [
      ...marks.map(mark => ({ pos: mark.from, start: true })),
      ...marks.map(mark => ({ pos: mark.to, start: false }))
    ],
    mark => mark.pos,
    mark => Number(!mark.start)
  );

  const cuts = [];
  let start = 0;
  let balance = 0;

  for (const mark of sortedMarks) {
    if (mark.start) {
      if (balance === 0 && start !== mark.pos) {
        cuts.push({ text: str.substring(start, mark.pos), marked: false });
        start = mark.pos;
      }
      balance++;
    } else {
      balance--;
      if (balance === 0 && start !== mark.pos) {
        cuts.push({ text: str.substring(start, mark.pos), marked: true });
        start = mark.pos;
      }
    }
  }

  if (start < str.length - 1) {
    cuts.push({ text: str.substr(start), marked: false });
  }

  return cuts;
}
