import React from 'react';

interface Props {
  value: number;
  label: string;
}

export const HeatmapNode: React.FC<Props> = ({ value, label }) => {
  const getIntensity = (val: number) => Math.min(255, Math.floor(val * 2));
  return (
    <div style={{
      width: '100px',
      height: '100px',
      backgroundColor: `rgb(${getIntensity(value)}, 100, 100)`,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      color: 'white',
      margin: '10px'
    }}>
      {label}: {value.toFixed(1)}
    </div>
  );
};
