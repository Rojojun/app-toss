import { useEffect, useState, type MouseEvent } from "react";
import { Button, NumericSpinner, TextField, useBottomSheet } from "@toss/tds-mobile";

const MIN = 0;
const MAX = 9999;

const clamp = (value: number): number => Math.min(MAX, Math.max(MIN, value));

function InputSheet({ initial, title, onSubmit }: { readonly initial: number; readonly title: string; readonly onSubmit: (value: number) => void }) {
  const [text, setText] = useState(String(initial));
  const [keyboardHeight, setKeyboardHeight] = useState(0);
  const digits = text.replace(/\D/g, "");
  const value = digits === "" ? null : clamp(Number(digits));

  useEffect(() => {
    const viewport = window.visualViewport;
    if (!viewport) return;
    const update = () => setKeyboardHeight(Math.max(0, window.innerHeight - viewport.height - viewport.offsetTop));
    viewport.addEventListener("resize", update);
    viewport.addEventListener("scroll", update);
    update();
    return () => {
      viewport.removeEventListener("resize", update);
      viewport.removeEventListener("scroll", update);
    };
  }, []);

  return (
    <div className="stack" style={{ paddingBottom: 24, transform: `translateY(-${keyboardHeight}px)` }}>
      <h3 className="sheet-title">{title}</h3>
      <TextField
        variant="box"
        label="숫자"
        labelOption="sustain"
        inputMode="numeric"
        autoFocus
        value={text}
        maxLength={4}
        onFocus={(event) => event.target.select()}
        onClick={(event) => event.currentTarget.select()}
        onMouseUp={(event) => event.preventDefault()}
        onChange={(event) => setText(event.target.value.replace(/\D/g, ""))}
      />
      <div className="pad">
        <Button display="block" disabled={value === null} onClick={() => value !== null && onSubmit(value)}>확인</Button>
      </div>
    </div>
  );
}

// −/+ 는 그대로 두고, 가운데 숫자를 누르면 숫자를 직접 입력하는 시트를 연다.
export function QuantitySpinner({
  size,
  number,
  title = "수량 입력",
  onNumberChange,
}: {
  readonly size: "tiny" | "small" | "medium" | "large";
  readonly number: number;
  readonly title?: string;
  readonly onNumberChange: (value: number) => void;
}) {
  const sheet = useBottomSheet();

  const openInput = (event: MouseEvent<HTMLDivElement>) => {
    if ((event.target as HTMLElement).closest("button")) return;
    event.stopPropagation();
    sheet.open({
      children: (
        <InputSheet
          initial={number}
          title={title}
          onSubmit={(value) => {
            sheet.close();
            if (value !== number) onNumberChange(value);
          }}
        />
      ),
      onClose: sheet.close,
    });
  };

  return (
    <div onClick={openInput} style={{ cursor: "text" }}>
      <NumericSpinner size={size} number={number} minNumber={MIN} maxNumber={MAX} onNumberChange={onNumberChange} />
    </div>
  );
}
